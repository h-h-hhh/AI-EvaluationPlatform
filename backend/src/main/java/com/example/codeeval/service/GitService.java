package com.example.codeeval.service;

import com.example.codeeval.entity.*;
import com.example.codeeval.repository.*;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GitService {

    // =====================================================
    // GitHub 公开仓库 URL 解析正则：
    //   捕获 1 : owner
    //   捕获 2 : repo（不含 .git 后缀）
    //   捕获 5 : 可选子路径 /tree/... 或 /blob/... 中的分支名（若存在）
    // 支持的 URL 形式：
    //   https://github.com/user/repo
    //   https://github.com/user/repo.git
    //   http://github.com/user/repo
    //   https://github.com/user/repo/tree/main
    //   https://www.github.com/user/repo
    // =====================================================
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https?://(?:www\\.)?github\\.com/([^/\\s]+)/([^/\\s.]+?)(?:\\.git)?((/tree/([^/\\s]+))?(/.*)?)?$",
            Pattern.CASE_INSENSITIVE);

    /** 受信任的代码文件扩展名；zipball 中仅这些文件会被合并到 CodeSubmission.codeContent */
    private static final Set<String> SUPPORTED_CODE_EXTENSIONS = Set.of(
            ".java", ".py", ".js", ".jsx", ".ts", ".tsx",
            ".cpp", ".cc", ".cxx", ".c", ".h", ".hpp", ".hh",
            ".cs", ".go", ".rs", ".kt", ".scala", ".rb", ".php",
            ".swift", ".m", ".mm", ".dart", ".sql", ".sh",
            ".vue", ".svelte", ".html", ".css", ".scss", ".less",
            ".json", ".yaml", ".yml", ".toml", ".xml"
    );

    /** 单个代码文件读取大小上限（UTF-8 字符），避免恶意二进制 / 大文件拖垮内存 */
    private static final int MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB / 文件

    /** 合并后整份 codeContent 的最大字符数上限（防止极端大仓库撑爆 TEXT 列） */
    private static final int MAX_TOTAL_SIZE = 10 * 1024 * 1024; // 10 MB

    /** WebClient 下载 ZIP 时 TCP 连接与响应总超时；GitHub zipball 一般 1-8 秒完成 */
    private static final int GITHUB_DOWNLOAD_CONNECT_TIMEOUT_MS = 10000;
    private static final int GITHUB_DOWNLOAD_RESPONSE_TIMEOUT_MS = 60000;

    /** WebClient 全局单例（Spring 不建议为每次请求新建） */
    private final WebClient githubWebClient;

    @Autowired
    private CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    public GitService() {
        // 构造专用 WebClient：无基础 URL、无鉴权（访问公开仓库即可），
        // 仅对连接/响应设置合理的短超时，避免请求挂死。
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, GITHUB_DOWNLOAD_CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofMillis(GITHUB_DOWNLOAD_RESPONSE_TIMEOUT_MS))
                // 关键：GitHub zipball 端点（api.github.com/repos/.../zipball/...）会返回
                // 302 重定向到 codeload.github.com 的真实 ZIP 地址。
                // 注意：Reactor Netty 与 JDK HttpURLConnection 不同，【默认不跟随重定向】，
                // 若不显式开启 followRedirect，bodyToMono(byte[].class) 只会拿到空的 302 响应体，
                // 表现为"ZIP 下载为空"。这里必须显式开启自动跟随 3xx。
                .followRedirect(true);
        this.githubWebClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, "CodeEval-Platform/1.0 (+https://github.com)")
                .defaultHeader(HttpHeaders.ACCEPT, "application/zip,application/octet-stream,*/*")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(256 * 1024 * 1024)) // zipball 缓冲区：最大 256 MB
                .build();
    }

    // ============================================================================
    // 原有历史功能（学生 Git 提交历史、仓库分析、URL 校验）保持不变、可复用。
    // ============================================================================

    /**
     * 获取学生某次作业的Git提交历史
     */
    public Map<String, Object> getStudentSubmissionHistory(Long studentId, Long assignmentId) {
        Map<String, Object> result = new HashMap<>();

        List<CodeSubmission> submissions = codeSubmissionRepository.findByStudentId(studentId);
        submissions = submissions.stream()
                .filter(s -> s.getAssignment() != null && s.getAssignment().getId().equals(assignmentId))
                .collect(Collectors.toList());

        result.put("commitCount", submissions.size());
        result.put("submissions", submissions.stream().map(this::convertToDto).collect(Collectors.toList()));

        // 计算提交频率
        if (submissions.size() > 1) {
            submissions.sort(Comparator.comparing(CodeSubmission::getSubmittedAt));
            LocalDateTime first = submissions.get(0).getSubmittedAt();
            LocalDateTime last = submissions.get(submissions.size() - 1).getSubmittedAt();
            long days = java.time.temporal.ChronoUnit.DAYS.between(first, last);
            double frequency = days > 0 ? (double) submissions.size() / days : submissions.size();
            result.put("commitFrequency", frequency > 2 ? "高" : frequency > 0.5 ? "中" : "低");
        } else {
            result.put("commitFrequency", submissions.isEmpty() ? "无" : "低");
        }

        return result;
    }

    /**
     * 获取仓库分析信息
     */
    public Map<String, Object> getRepositoryAnalysis(Long assignmentId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Assignment> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isEmpty()) {
            return result;
        }

        Assignment assignment = assignmentOpt.get();
        result.put("title", assignment.getTitle());
        result.put("description", assignment.getDescription());
        result.put("totalScore", assignment.getTotalScore());
        result.put("deadline", assignment.getDeadline());

        List<CodeSubmission> submissions = codeSubmissionRepository.findByAssignmentId(assignmentId);
        result.put("totalSubmissions", submissions.size());
        result.put("submittedStudents", submissions.stream()
                .map(CodeSubmission::getStudent)
                .filter(Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toSet()).size());

        return result;
    }

    /**
     * 验证Git仓库URL格式（兼容老前端）
     */
    public Map<String, Object> validateGitUrl(String repoUrl, String repoType) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        result.put("message", "");

        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            result.put("message", "仓库URL不能为空");
            return result;
        }

        // 简单的URL格式验证
        String httpsPrefix = "";
        switch (repoType == null ? "" : repoType) {
            case "github":
                httpsPrefix = "https://github.com/";
                break;
            case "gitlab":
                httpsPrefix = "https://gitlab.com/";
                break;
            case "gitee":
                httpsPrefix = "https://gitee.com/";
                break;
            default:
                // 自定义仓库不验证前缀
                result.put("valid", true);
                return result;
        }

        if (repoUrl.startsWith(httpsPrefix)) {
            result.put("valid", true);
        } else if (repoType.equals("github") && GITHUB_URL_PATTERN.matcher(repoUrl.trim()).matches()) {
            // 允许 http 或带 www 前缀，使用严格正则再校验一次
            result.put("valid", true);
        } else if (repoUrl.startsWith("http://") || repoUrl.startsWith("https://")) {
            result.put("message", "URL格式不正确，应为 " + httpsPrefix + "xxx/repo");
        } else {
            result.put("message", "URL必须以 " + httpsPrefix + " 开头");
        }

        return result;
    }

    private Map<String, Object> convertToDto(CodeSubmission submission) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", submission.getId());
        dto.put("commitHash", submission.getGitCommitHash());
        dto.put("submitTime", submission.getSubmittedAt());
        dto.put("repoUrl", submission.getRepositoryUrl());
        dto.put("githubUrl", submission.getGithubUrl());
        dto.put("githubBranch", submission.getGithubBranch());
        dto.put("submitCount", submission.getSubmitCount());
        dto.put("isLatest", submission.getIsLatest());
        return dto;
    }

    // ============================================================================
    // 【新增 GitHub 作业提交】解析 URL → 下载 zipball → 解压 → 合并代码
    // ============================================================================

    /**
     * GitHub URL 解析结果。
     * owner / repo / normalizedRepoUrl 一定非空；branch 可为空（调用者再做 main/master 回退）。
     */
    public static class GithubRepoInfo {
        public final String owner;
        public final String repo;
        public final String branch;        // null 表示 URL 未显式指定分支
        public final String normalizedRepoUrl; // 规范化的仓库首页 URL，用于教师端链接跳转

        public GithubRepoInfo(String owner, String repo, String branch, String normalizedRepoUrl) {
            this.owner = owner;
            this.repo = repo;
            this.branch = branch;
            this.normalizedRepoUrl = normalizedRepoUrl;
        }
    }

    /**
     * 解析 GitHub 公开仓库 URL。
     *
     * @throws IllegalArgumentException 格式不匹配时抛出，由上层通过 GlobalExceptionHandler 包装为 ApiResponse。
     */
    public GithubRepoInfo parseGithubUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("GitHub 仓库 URL 不能为空");
        }
        String url = rawUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        Matcher m = GITHUB_URL_PATTERN.matcher(url);
        if (!m.matches()) {
            throw new IllegalArgumentException("GitHub 仓库 URL 格式不正确，应为 https://github.com/{user}/{repo}[/{optional-path}]");
        }
        String owner = m.group(1);
        String repo = m.group(2);
        String branch = m.group(5); // /tree/<branch> 中捕获的分支，未指定为 null
        String normalized = String.format("https://github.com/%s/%s", owner, repo);
        return new GithubRepoInfo(owner, repo, branch, normalized);
    }

    /**
     * 【核心入口】根据 GitHub URL 下载公开仓库 ZIP 并合并代码内容。
     *
     * 流程：
     *  1. parseGithubUrl：正则解析 owner/repo/(branch)
     *  2. 分支选择：学生指定 > URL /tree/xxx > 尝试 main > 尝试 master
     *  3. GET https://api.github.com/repos/{owner}/{repo}/zipball/{branch}：302 跳转到 codeload
     *  4. 读取响应流 byte[] → ZIP 解压
     *  5. 跳过目录/二进制/非代码扩展名/超过 2MB 单文件，按扩展名白名单过滤
     *  6. 以 "===== {相对路径} =====\n{内容}\n\n" 形式拼接为 codeContent 文本
     *  7. 返回 FetchedResult（mergedCode + 分支 + 规范化 URL）
     *
     * @param rawUrl   学生填写的 GitHub 仓库 URL
     * @param preferredBranch 学生填写的分支名（可为空）；优先级高于 URL 中捕获的 branch
     * @return 合并后的代码文本 + 实际使用的分支 + 规范化仓库地址
     * @throws IllegalArgumentException 下载失败 / 解压失败 / 空仓库 / 无可用代码文件
     */
    public FetchedResult fetchGithubRepoContent(String rawUrl, String preferredBranch) {
        GithubRepoInfo info = parseGithubUrl(rawUrl);

        // 分支选择优先级：显式填写 > URL /tree/branch 捕获 > main > master
        List<String> branchCandidates = new ArrayList<>();
        if (preferredBranch != null && !preferredBranch.isBlank()) {
            branchCandidates.add(preferredBranch.trim());
        }
        if (info.branch != null && !info.branch.isBlank()) {
            if (branchCandidates.isEmpty() || !branchCandidates.get(0).equals(info.branch)) {
                branchCandidates.add(info.branch);
            }
        }
        if (!branchCandidates.contains("main")) branchCandidates.add("main");
        if (!branchCandidates.contains("master")) branchCandidates.add("master");

        String lastErrorMessage = null;
        for (String branch : branchCandidates) {
            try {
                return downloadAndExtract(info, branch);
            } catch (WebClientResponseException.NotFound nf) {
                lastErrorMessage = String.format("分支 %s 不存在（404），已尝试继续回退其它候选", branch);
            } catch (WebClientResponseException.Forbidden | WebClientResponseException.TooManyRequests rateLimit) {
                // Java 17 兼容写法：rateLimit 是 multi-catch 变量，
                // 两个类型都是 WebClientResponseException 的子类，可直接多态调用 getStatusCode()
                int status;
                if (rateLimit instanceof WebClientResponseException) {
                    status = ((WebClientResponseException) rateLimit).getStatusCode().value();
                } else {
                    status = 429;
                }
                String message = String.format("GitHub API 限流或拒绝访问（HTTP %d），请稍后重试或改用粘贴代码提交", status);
                throw new IllegalArgumentException(message, rateLimit);
            } catch (WebClientResponseException wcre) {
                HttpStatus status = (HttpStatus) wcre.getStatusCode();
                if (status.is4xxClientError()) {
                    lastErrorMessage = String.format("分支 %s 访问失败（HTTP %d），回退下一候选", branch, status.value());
                    continue;
                }
                throw new IllegalArgumentException(
                        String.format("GitHub 仓库下载失败（HTTP %d）：%s", status.value(), wcre.getMessage()), wcre);
            } catch (IOException io) {
                lastErrorMessage = String.format("分支 %s ZIP 解压失败：%s", branch, io.getMessage());
            } catch (IllegalStateException ise) {
                // 单分支无可用代码的 IllegalStateException：回退下一候选
                lastErrorMessage = ise.getMessage();
            } catch (RuntimeException re) {
                // 其它运行时异常（如 WebClient timeout / netty 连接失败）：不再回退
                throw new IllegalArgumentException("GitHub 仓库下载失败：" + re.getMessage(), re);
            }
        }

        // 全部候选都失败；各分支的 lastErrorMessage 可能自带句末句号，拼接前先去掉，避免出现"。。"
        String prefix = "下载 GitHub 仓库失败。";
        if (lastErrorMessage != null) {
            String detail = lastErrorMessage.trim();
            while (detail.endsWith("。")) {
                detail = detail.substring(0, detail.length() - 1).trim();
            }
            prefix += "最后错误：" + detail + "。";
        }
        prefix += "请检查仓库是否公开、URL 是否正确，或改用粘贴代码提交。";
        throw new IllegalArgumentException(prefix);
    }

    /** 执行单分支的 zipball 下载 + 解压 + 合并 */
    private FetchedResult downloadAndExtract(GithubRepoInfo info, String branch) throws IOException {
        // 使用 api.github.com 的 zipball 端点：会 302 重定向到 codeload.github.com 真实 ZIP 地址。
        // 重定向跟随依赖构造 githubWebClient 时配置的 HttpClient.followRedirect(true)，
        // Reactor Netty 默认不跟随 3xx，切勿移除该配置。
        String uri = String.format("https://api.github.com/repos/%s/%s/zipball/%s",
                info.owner, info.repo, branch);

        byte[] zipBytes;
        try {
            zipBytes = githubWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(Duration.ofMillis(GITHUB_DOWNLOAD_RESPONSE_TIMEOUT_MS + 5000L));
        } catch (RuntimeException re) {
            // 所有 WebClient 相关异常（超时 / 连接失败 / HTTP 错误）统一抛出，
            // 由上层 fetchGithubRepoContent 捕获分类后决定是否回退分支。
            throw re;
        }

        if (zipBytes == null || zipBytes.length == 0) {
            throw new IllegalStateException("ZIP 下载为空，可能是空仓库或分支无文件。");
        }

        // TreeMap 使拼接顺序稳定（按相对路径字典序），便于每次下载同样的仓库得到相同 codeContent
        Map<String, String> codeFiles = new TreeMap<>();
        long totalChars = 0;
        int skippedCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                // ZIP 第一层是 github zipball 自动生成的 {owner}-{repo}-{sha}/ 根目录，
                // 去掉该前缀后作为展示用的相对路径（更友好）。
                String rawName = entry.getName();
                String relativePath = stripZipballRoot(rawName);

                String ext = getExtension(relativePath);
                if (ext == null || !SUPPORTED_CODE_EXTENSIONS.contains(ext)) {
                    skippedCount++;
                    zis.closeEntry();
                    continue;
                }

                // 读取条目内容到 String；限定单文件 2MB 上限，
                // 超过则截断并追加说明，避免大仓库的二进制/大 JS bundle 撑满内存。
                String content = readEntryAsString(zis, entry, MAX_FILE_SIZE);
                if (content.isEmpty()) {
                    zis.closeEntry();
                    continue;
                }

                // 总体字符数限制：超过则停止继续追加文件（已读取的保留）
                if (totalChars + content.length() + relativePath.length() + 50L > MAX_TOTAL_SIZE) {
                    skippedCount++;
                    zis.closeEntry();
                    break;
                }
                codeFiles.put(relativePath, content);
                totalChars += content.length();
                zis.closeEntry();
            }
        }

        if (codeFiles.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "仓库中没有识别到可用源代码文件（支持 %d 种常见扩展名），请确认仓库内容或改用粘贴代码提交。",
                    SUPPORTED_CODE_EXTENSIONS.size()));
        }

        StringBuilder sb = new StringBuilder((int) Math.min(MAX_TOTAL_SIZE, totalChars + codeFiles.size() * 80L));
        for (Map.Entry<String, String> e : codeFiles.entrySet()) {
            sb.append("===== FILE: ").append(e.getKey()).append(" =====\n");
            sb.append(e.getValue());
            if (!e.getValue().endsWith("\n")) sb.append("\n");
            sb.append("\n");
        }

        FetchedResult result = new FetchedResult();
        result.mergedCodeContent = sb.toString();
        result.actualBranch = branch;
        result.normalizedRepoUrl = info.normalizedRepoUrl;
        result.fileCount = codeFiles.size();
        result.skippedCount = skippedCount;
        result.owner = info.owner;
        result.repo = info.repo;
        return result;
    }

    /** 返回结果封装 */
    public static class FetchedResult {
        /** 合并后的多文件代码文本，可直接写入 CodeSubmission.codeContent */
        public String mergedCodeContent;
        /** 实际用于下载的分支名（学生指定/main/master 中的命中值） */
        public String actualBranch;
        /** 规范化仓库首页 URL（不含 /tree 等子路径） */
        public String normalizedRepoUrl;
        /** 成功抽取的代码文件数 */
        public int fileCount;
        /** 被跳过的条目数（二进制 / 不支持扩展名 / 超限） */
        public int skippedCount;
        public String owner;
        public String repo;
    }

    // ============================================================================
    // 私有工具方法
    // ============================================================================

    /** 去除 zipball 自动生成的根目录前缀；若无法识别则返回原名 */
    private static String stripZipballRoot(String entryName) {
        if (entryName == null) return "";
        int first = entryName.indexOf('/');
        if (first < 0) return entryName;
        return entryName.substring(first + 1);
    }

    /** 获取小写扩展名（包含点）；无扩展名返回 null */
    private static String getExtension(String fileName) {
        if (fileName == null) return null;
        int lastDot = fileName.lastIndexOf('.');
        int lastSep = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (lastDot <= lastSep || lastDot == fileName.length() - 1) return null;
        return fileName.substring(lastDot).toLowerCase(Locale.ROOT);
    }

    /** 从 ZipInputStream 读取当前 entry 为字符串，单文件超限时附加截断说明 */
    private static String readEntryAsString(ZipInputStream zis, ZipEntry entry, int maxBytes) throws IOException {
        long size = entry.getSize();
        int cap = size > 0 ? (int) Math.min(size, maxBytes) : 8192;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(cap);
        byte[] buf = new byte[8192];
        int totalRead = 0;
        int n;
        while ((n = zis.read(buf)) != -1) {
            if (totalRead + n > maxBytes) {
                int allowed = Math.max(0, maxBytes - totalRead);
                if (allowed > 0) baos.write(buf, 0, allowed);
                baos.write(("\n\n... [CodeEval 自动截断：该文件超过 " + maxBytes / 1024 + " KB 上限] ...\n")
                        .getBytes(StandardCharsets.UTF_8));
                break;
            }
            baos.write(buf, 0, n);
            totalRead += n;
        }
        String s = baos.toString(StandardCharsets.UTF_8);
        // 纯二进制文件：大量 '\0' 或不可打印字符占比 > 30% 时视为二进制，返回空串跳过。
        if (looksBinary(s)) return "";
        return s;
    }

    /** 是否疑似二进制内容：NUL 字节或可打印比例 < 70% 视为二进制 */
    private static boolean looksBinary(String s) {
        if (s.isEmpty()) return false;
        int nonPrintable = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\0') return true;
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r' && c != '\f' && c != '\u000b') {
                nonPrintable++;
            }
        }
        return (double) nonPrintable / s.length() > 0.30;
    }
}
