# INVENTORY — szymtrener
> AUTO-GENEROWANY 2026-08-26 przez tools/index_project.py — NIE edytuj ręcznie.
> To spis tego, co JUŻ ISTNIEJE. Grepuj go zanim zbudujesz coś nowego.
> Beany: 42 · Endpointy: 48 · Metody: 640 · Front: 0

## Beany / komponenty Spring
@Component      AdminNav — src/main/java/pl/szymtrener/admin/AdminNav.java
@Component      AnalyticsFilter — src/main/java/pl/szymtrener/analytics/AnalyticsFilter.java
@Component      AnalyticsView — src/main/java/pl/szymtrener/analytics/AnalyticsView.java
@Component      CleanupScheduler — src/main/java/pl/szymtrener/scheduler/CleanupScheduler.java
@Component      ContentMetrics — src/main/java/pl/szymtrener/content/ContentMetrics.java
@Component      DatabaseUrlEnvironmentPostProcessor — src/main/java/pl/szymtrener/config/DatabaseUrlEnvironmentPostProcessor.java
@Component      DocxToHtmlConverter — src/main/java/pl/szymtrener/docimport/DocxToHtmlConverter.java
@Component      EditorHtml — src/main/java/pl/szymtrener/content/EditorHtml.java
@Component      HtmlSanitizer — src/main/java/pl/szymtrener/content/HtmlSanitizer.java
@Component      LegacyDocConverter — src/main/java/pl/szymtrener/docimport/LegacyDocConverter.java
@Component      PostPageModel — src/main/java/pl/szymtrener/web/PostPageModel.java
@Component      PublishScheduler — src/main/java/pl/szymtrener/scheduler/PublishScheduler.java
@Component      RateLimiter — src/main/java/pl/szymtrener/submission/RateLimiter.java
@Configuration  AdminAccountInitializer — src/main/java/pl/szymtrener/admin/AdminAccountInitializer.java
@Configuration  MailConfig — src/main/java/pl/szymtrener/config/MailConfig.java
@Configuration  SecurityConfig — src/main/java/pl/szymtrener/config/SecurityConfig.java
@Configuration  WebConfig — src/main/java/pl/szymtrener/config/WebConfig.java
@Controller     AdminController — src/main/java/pl/szymtrener/admin/AdminController.java
@Controller     AdminMediaController — src/main/java/pl/szymtrener/admin/AdminMediaController.java
@Controller     AdminPostController — src/main/java/pl/szymtrener/admin/AdminPostController.java
@Controller     AdminSettingsController — src/main/java/pl/szymtrener/admin/AdminSettingsController.java
@Controller     AdminStatsController — src/main/java/pl/szymtrener/admin/AdminStatsController.java
@Controller     AdminSubmissionController — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
@Controller     AdminTraineeController — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
@Controller     BlogController — src/main/java/pl/szymtrener/web/BlogController.java
@Controller     HomeController — src/main/java/pl/szymtrener/web/HomeController.java
@RestController AdminApiController — src/main/java/pl/szymtrener/admin/AdminApiController.java
@RestController MediaController — src/main/java/pl/szymtrener/media/MediaController.java
@RestController PublicFormController — src/main/java/pl/szymtrener/web/PublicFormController.java
@RestController SeoController — src/main/java/pl/szymtrener/seo/SeoController.java
@Service        AdminUserDetailsService — src/main/java/pl/szymtrener/admin/AdminUserDetailsService.java
@Service        AiReadinessAnalyzer — src/main/java/pl/szymtrener/seo/AiReadinessAnalyzer.java
@Service        DocImportService — src/main/java/pl/szymtrener/docimport/DocImportService.java
@Service        IndexNowService — src/main/java/pl/szymtrener/seo/IndexNowService.java
@Service        JsonLdService — src/main/java/pl/szymtrener/seo/JsonLdService.java
@Service        MailService — src/main/java/pl/szymtrener/submission/MailService.java
@Service        MediaService — src/main/java/pl/szymtrener/media/MediaService.java
@Service        PostService — src/main/java/pl/szymtrener/content/PostService.java
@Service        SeoScoreService — src/main/java/pl/szymtrener/seo/SeoScoreService.java
@Service        SettingsService — src/main/java/pl/szymtrener/settings/SettingsService.java
@Service        SubmissionService — src/main/java/pl/szymtrener/submission/SubmissionService.java
@Service        TraineeService — src/main/java/pl/szymtrener/crm/TraineeService.java

## Endpointy REST
GET     /                                             HomeController.home() — src/main/java/pl/szymtrener/web/HomeController.java
GET     /admin                                        AdminController.dashboard() — src/main/java/pl/szymtrener/admin/AdminController.java
GET     /admin/api/media                              AdminApiController.list() — src/main/java/pl/szymtrener/admin/AdminApiController.java
GET     /admin/haslo                                  AdminController.passwordForm() — src/main/java/pl/szymtrener/admin/AdminController.java
GET     /admin/klienci                                AdminTraineeController.list() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
GET     /admin/klienci/nowy                           AdminTraineeController.create() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
GET     /admin/klienci/{id}                           AdminTraineeController.edit() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
GET     /admin/logowanie                              AdminController.login() — src/main/java/pl/szymtrener/admin/AdminController.java
GET     /admin/media                                  AdminMediaController.library() — src/main/java/pl/szymtrener/admin/AdminMediaController.java
GET     /admin/posty                                  AdminPostController.list() — src/main/java/pl/szymtrener/admin/AdminPostController.java
GET     /admin/posty/nowy                             AdminPostController.create() — src/main/java/pl/szymtrener/admin/AdminPostController.java
GET     /admin/posty/{id}                             AdminPostController.edit() — src/main/java/pl/szymtrener/admin/AdminPostController.java
GET     /admin/posty/{id}/podglad                     AdminPostController.preview() — src/main/java/pl/szymtrener/admin/AdminPostController.java
GET     /admin/statystyki                             AdminStatsController.stats() — src/main/java/pl/szymtrener/admin/AdminStatsController.java
GET     /admin/ustawienia                             AdminSettingsController.form() — src/main/java/pl/szymtrener/admin/AdminSettingsController.java
GET     /admin/zgloszenia                             AdminSubmissionController.list() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
GET     /admin/zgloszenia/{id}                        AdminSubmissionController.detail() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
GET     /admin/zgloszenia/{id}/dane                   AdminSubmissionController.export() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
GET     /blog                                         BlogController.list() — src/main/java/pl/szymtrener/web/BlogController.java
GET     /blog/kategoria/{slug}                        BlogController.byCategory() — src/main/java/pl/szymtrener/web/BlogController.java
GET     /blog/szukaj                                  BlogController.search() — src/main/java/pl/szymtrener/web/BlogController.java
GET     /blog/{slug}                                  BlogController.post() — src/main/java/pl/szymtrener/web/BlogController.java
GET     /feed.xml                                     SeoController.feed() — src/main/java/pl/szymtrener/seo/SeoController.java
GET     /llms.txt                                     SeoController.llms() — src/main/java/pl/szymtrener/seo/SeoController.java
GET     /media/**                                     MediaController.serve() — src/main/java/pl/szymtrener/media/MediaController.java
GET     /pliki/{id}/**                                MediaController.download() — src/main/java/pl/szymtrener/media/MediaController.java
GET     /polityka-prywatnosci                         HomeController.privacy() — src/main/java/pl/szymtrener/web/HomeController.java
GET     /robots.txt                                   SeoController.robots() — src/main/java/pl/szymtrener/seo/SeoController.java
GET     /sitemap.xml                                  SeoController.sitemap() — src/main/java/pl/szymtrener/seo/SeoController.java
GET     /{key}.txt                                    SeoController.indexNowKey() — src/main/java/pl/szymtrener/seo/SeoController.java
POST    /admin/api/import-docx                        AdminApiController.importDocument() — src/main/java/pl/szymtrener/admin/AdminApiController.java
POST    /admin/api/media                              AdminApiController.upload() — src/main/java/pl/szymtrener/admin/AdminApiController.java
POST    /admin/haslo                                  AdminController.changePassword() — src/main/java/pl/szymtrener/admin/AdminController.java
POST    /admin/klienci/zapisz                         AdminTraineeController.save() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/ze-zgloszenia/{submissionId}   AdminTraineeController.fromSubmission() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/{id}/usun                      AdminTraineeController.delete() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/media/{id}/usun                        AdminMediaController.delete() — src/main/java/pl/szymtrener/admin/AdminMediaController.java
POST    /admin/posty/autozapis                        AdminPostController.autosave() — src/main/java/pl/szymtrener/admin/AdminPostController.java
POST    /admin/posty/ocena                            AdminPostController.score() — src/main/java/pl/szymtrener/admin/AdminPostController.java
POST    /admin/posty/zapisz                           AdminPostController.save() — src/main/java/pl/szymtrener/admin/AdminPostController.java
POST    /admin/posty/{id}/usun                        AdminPostController.delete() — src/main/java/pl/szymtrener/admin/AdminPostController.java
POST    /admin/ustawienia                             AdminSettingsController.save() — src/main/java/pl/szymtrener/admin/AdminSettingsController.java
POST    /admin/ustawienia/test-poczty                 AdminSettingsController.testMail() — src/main/java/pl/szymtrener/admin/AdminSettingsController.java
POST    /admin/zgloszenia/{id}/notatka                AdminSubmissionController.addNote() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/status                 AdminSubmissionController.changeStatus() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/usun                   AdminSubmissionController.delete() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /api/zgloszenia/kontakt                       PublicFormController.contact() — src/main/java/pl/szymtrener/web/PublicFormController.java
POST    /api/zgloszenia/online                        PublicFormController.online() — src/main/java/pl/szymtrener/web/PublicFormController.java

## Metody (per klasa)

### AdminAccountInitializer  (src/main/java/pl/szymtrener/admin/AdminAccountInitializer.java)
- [   ] createAdminIfMissing(AdminUserRepository repo, PasswordEncoder encoder, AppProperties props): ApplicationRunner  :26

### AdminApiController  (src/main/java/pl/szymtrener/admin/AdminApiController.java)
- [prv] describe(MediaFile file): Map<String, Object>  :62

### AdminController  (src/main/java/pl/szymtrener/admin/AdminController.java)
- [pub] dashboard(Model model): String  :56
- [prv] queue(): List<Post>  :91
- [pub] login(): String  :100
- [pub] passwordForm(Model model): String  :105
- [prv] validate(PasswordForm form, AdminUser user): String  :141

### AdminMediaController  (src/main/java/pl/szymtrener/admin/AdminMediaController.java)
- [prv] humanSize(long bytes): String  :54
- [pub] delete(@PathVariable Long id, RedirectAttributes flash): String  :67

### AdminNav  (src/main/java/pl/szymtrener/admin/AdminNav.java)
- [pub] publishedPosts(): long  :30
- [pub] newSubmissions(): long  :35

### AdminPostController  (src/main/java/pl/szymtrener/admin/AdminPostController.java)
- [prv] calendar(YearMonth month): List<CalendarDay>  :93
- [pub] create(Model model): String  :121
- [pub] edit(@PathVariable Long id, Model model): String  :139
- [prv] persist(PostForm form): Post  :202
- [pub] preview(@PathVariable Long id, Model model): String  :260
- [pub] delete(@PathVariable Long id, RedirectAttributes flash): String  :269
- [pub] score(@RequestBody PostForm form): SeoScoreService.Result  :286
- [prv] draft(PostForm form): Post  :296
- [prv] toForm(Post post): PostForm  :323

### AdminSettingsController  (src/main/java/pl/szymtrener/admin/AdminSettingsController.java)
- [pub] form(Model model): String  :43
- [pub] testMail(RedirectAttributes flash): String  :65

### AdminSubmissionController  (src/main/java/pl/szymtrener/admin/AdminSubmissionController.java)
- [pub] detail(@PathVariable Long id, Model model): String  :64
- [pub] addNote(@PathVariable Long id, @RequestParam String body, Principal principal): String  :84
- [pub] export(@PathVariable Long id): ResponseEntity<byte[]>  :95
- [pub] delete(@PathVariable Long id, Principal principal): String  :111

### AdminTraineeController  (src/main/java/pl/szymtrener/admin/AdminTraineeController.java)
- [pub] create(Model model): String  :47
- [pub] edit(@PathVariable Long id, Model model): String  :54
- [pub] delete(@PathVariable Long id, RedirectAttributes flash): String  :70
- [pub] fromSubmission(@PathVariable Long submissionId, RedirectAttributes flash): String  :78
- [prv] form(Model model, Trainee trainee): String  :84
- [prv] toForm(Trainee trainee): TraineeForm  :93

### AdminUser  (src/main/java/pl/szymtrener/admin/AdminUser.java)
- [pub] getId(): Long  :33
- [pub] getEmail(): String  :34
- [pub] setEmail(String email): void  :35
- [pub] getPasswordHash(): String  :36
- [pub] setPasswordHash(String passwordHash): void  :37
- [pub] getDisplayName(): String  :38
- [pub] setDisplayName(String displayName): void  :39
- [pub] getRole(): String  :40
- [pub] setRole(String role): void  :41
- [pub] isEnabled(): boolean  :42
- [pub] setEnabled(boolean enabled): void  :43
- [pub] getLastLoginAt(): Instant  :44
- [pub] setLastLoginAt(Instant lastLoginAt): void  :45
- [pub] getCreatedAt(): Instant  :46

### AdminUserDetailsService  (src/main/java/pl/szymtrener/admin/AdminUserDetailsService.java)
- [pub] loadUserByUsername(String username): UserDetails  :17
- [   ] empty(): InMemoryUserDetailsManager  :28

### AdminUserRepository  (src/main/java/pl/szymtrener/admin/AdminUserRepository.java)
- [   ] findByEmailIgnoreCase(String email): Optional<AdminUser>  :7

### AiReadinessAnalyzer  (src/main/java/pl/szymtrener/seo/AiReadinessAnalyzer.java)
- [pub] analyse(Post post): Report  :37
- [prv] answerFirst(Post post): Check  :58
- [prv] sectionAnswers(Document doc): Check  :72
- [prv] dataDensity(Document doc, Post post): Check  :93
- [prv] chunkFriendliness(Document doc): Check  :111
- [prv] citations(Document doc): Check  :129
- [prv] freshness(Post post): Check  :145
- [prv] questionHeadings(Document doc): Check  :156
- [prv] formatting(Document doc): Check  :164
- [prv] faq(Post post): Check  :173
- [prv] formatVariety(Document doc, Post post): Check  :181
- [prv] words(String text): int  :192

### AiReadinessAnalyzerTest  (src/test/java/pl/szymtrener/seo/AiReadinessAnalyzerTest.java)
- [prv] post(String lead, String html): Post  :24
- [prv] lead(int words): String  :33
- [   ] isDeterministic(): void  :39
- [   ] scoreStaysInRange(): void  :51
- [   ] emptyPostScoresLow(): void  :69
- [   ] goodPostBeatsWeakPost(): void  :78
- [prv] faq(): PostFaq  :97
- [   ] leadLength(): void  :110
- [   ] onlyAuthoritativeCitationsCount(): void  :121
- [   ] freshness(): void  :137
- [   ] faqCheck(): void  :148
- [   ] questionHeading(): void  :159
- [   ] wallOfTextIsPenalised(): void  :168
- [prv] check(Post post, String label): AiReadinessAnalyzer.Check  :177

### AnalyticsFilter  (src/main/java/pl/szymtrener/analytics/AnalyticsFilter.java)
- [pro] shouldNotFilter(HttpServletRequest request): boolean  :51
- [prv] record(HttpServletRequest request): void  :73
- [prv] clientIp(HttpServletRequest request): String  :91
- [prv] referrerHost(String referrer): String  :98
- [prv] hash(String input): String  :107

### AnalyticsView  (src/main/java/pl/szymtrener/analytics/AnalyticsView.java)
- [pub] bars(Instant since, int range): List<Bar>  :52
- [pub] rows(List<Object[]> source, long total, int limit): List<Row>  :76
- [pub] trend(long current, long previous): Trend  :92
- [pub] trendOverDays(int days, java.util.function.BiFunction<Instant, Instant, Long> counter): Trend  :101

### AppProperties  (src/main/java/pl/szymtrener/config/AppProperties.java)
- [pub] absolute(String path): String  :23

### AppSetting  (src/main/java/pl/szymtrener/settings/AppSetting.java)
- [pub] getKey(): String  :28
- [pub] getValue(): String  :29
- [pub] setValue(String value): void  :30
- [pub] getUpdatedAt(): Instant  :31

### ApplicationContextIT  (src/test/java/pl/szymtrener/ApplicationContextIT.java)
- [   ] contextLoads(): void  :24
- [   ] migrationsApplied(): void  :30
- [   ] fullTextSearchIsWired(): void  :42
- [   ] seedDataLoaded(): void  :56

### Author  (src/main/java/pl/szymtrener/content/Author.java)
- [pub] getId(): Long  :30
- [pub] getSlug(): String  :31
- [pub] setSlug(String slug): void  :32
- [pub] getName(): String  :33
- [pub] setName(String name): void  :34
- [pub] getJobTitle(): String  :35
- [pub] setJobTitle(String jobTitle): void  :36
- [pub] getBio(): String  :37
- [pub] setBio(String bio): void  :38
- [pub] getPhotoPath(): String  :39
- [pub] setPhotoPath(String photoPath): void  :40
- [pub] getEmail(): String  :41
- [pub] setEmail(String email): void  :42
- [pub] getSameAs(): Set<String>  :43

### AuthorRepository  (src/main/java/pl/szymtrener/content/AuthorRepository.java)
- [   ] findBySlug(String slug): Optional<Author>  :7

### BlogController  (src/main/java/pl/szymtrener/web/BlogController.java)
- [   ] foundLabel(long count): String  :51
- [prv] pageSize(): int  :60
- [prv] renderList(String categorySlug, int page, Model model): String  :77
- [pub] post(@PathVariable String slug, Model model): ModelAndView  :144
- [prv] redirectFromOldSlug(String slug): RedirectView  :158

### Category  (src/main/java/pl/szymtrener/content/Category.java)
- [pub] getId(): Long  :18
- [pub] getSlug(): String  :19
- [pub] setSlug(String slug): void  :20
- [pub] getName(): String  :21
- [pub] setName(String name): void  :22
- [pub] getDescription(): String  :23
- [pub] setDescription(String description): void  :24
- [pub] getSortOrder(): int  :25
- [pub] setSortOrder(int sortOrder): void  :26

### CategoryRepository  (src/main/java/pl/szymtrener/content/CategoryRepository.java)
- [   ] findBySlug(String slug): Optional<Category>  :8
- [   ] findAllByOrderBySortOrderAsc(): List<Category>  :9

### CleanupScheduler  (src/main/java/pl/szymtrener/scheduler/CleanupScheduler.java)
- [pub] purgeOldPageViews(): void  :32

### ContentMetrics  (src/main/java/pl/szymtrener/content/ContentMetrics.java)
- [pub] analyse(String contentHtml, String lead): Result  :15

### DatabaseUrlEnvironmentPostProcessor  (src/main/java/pl/szymtrener/config/DatabaseUrlEnvironmentPostProcessor.java)
- [pub] postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application): void  :34
- [   ] translate(String raw): Map<String, Object>  :52
- [prv] decode(String value): String  :92

### DatabaseUrlEnvironmentPostProcessorTest  (src/test/java/pl/szymtrener/config/DatabaseUrlEnvironmentPostProcessorTest.java)
- [   ] translatesPlainUrl(): void  :18
- [   ] decodesEncodedPassword(): void  :29
- [   ] defaultsPort(): void  :38
- [   ] keepsQueryString(): void  :47
- [   ] acceptsBothSchemes(): void  :57
- [   ] leavesJdbcUrlAlone(): void  :64
- [   ] garbageIsIgnored(): void  :71

### DocImportService  (src/main/java/pl/szymtrener/docimport/DocImportService.java)
- [pub] importDocument(MultipartFile file): ImportResult  :24

### DocxFixtures  (src/test/java/pl/szymtrener/docimport/DocxFixtures.java)
- [   ] bytes(XWPFDocument doc): InputStream  :23
- [   ] englishWord(): XWPFDocument  :31
- [   ] polishWord(): XWPFDocument  :51
- [   ] listsTableAndQuote(): XWPFDocument  :61
- [   ] htmlUnsafeText(): XWPFDocument  :88
- [   ] empty(): XWPFDocument  :94
- [prv] heading(XWPFDocument doc, String styleId, String text): void  :100
- [prv] plain(XWPFDocument doc, String text): void  :106
- [prv] run(XWPFParagraph p, String text, boolean bold, boolean italic, boolean underline, boolean strike): void  :110
- [prv] listItem(XWPFDocument doc, BigInteger numId, String text): void  :121
- [   ] orderedListWithoutIlvl(): XWPFDocument  :129
- [prv] numbering(XWPFDocument doc, STNumberFormat.Enum format, int abstractId): BigInteger  :143
- [   ] listWithoutNumberingDefinition(): XWPFDocument  :156

### DocxToHtmlConverter  (src/main/java/pl/szymtrener/docimport/DocxToHtmlConverter.java)
- [pub] convert(InputStream in, String sourceName): ImportResult  :34
- [prv] appendParagraph(XWPFDocument doc, XWPFParagraph p, StringBuilder html, ListState list, List<String> warnings, int[] images): void  :57
- [prv] renderRuns(XWPFDocument doc, XWPFParagraph p, List<String> warnings, int[] images): String  :84
- [prv] extractPictures(XWPFRun run, List<String> warnings, int[] images): String  :110
- [prv] appendTable(XWPFTable table, StringBuilder html): void  :138
- [prv] headingLevel(XWPFParagraph p): int  :158
- [prv] isQuote(XWPFParagraph p): boolean  :169
- [prv] isListItem(XWPFParagraph p): boolean  :174
- [prv] isOrdered(XWPFParagraph p): boolean  :178
- [prv] numberFormat(XWPFParagraph p): String  :193
- [prv] escape(String text): String  :231
- [   ] open(StringBuilder html, String tag): void  :238
- [   ] close(StringBuilder html): void  :244

### DocxToHtmlConverterTest  (src/test/java/pl/szymtrener/docimport/DocxToHtmlConverterTest.java)
- [   ] setUp(): void  :26
- [   ] mapsEnglishHeadingsOneLevelDown(): void  :36
- [   ] mapsPolishHeadings(): void  :46
- [   ] mapsCharacterFormatting(): void  :55
- [   ] mapsListsTableAndQuote(): void  :66
- [   ] dropsWordStyling(): void  :88
- [   ] escapesHtmlUnsafeText(): void  :98
- [   ] handlesEmptyDocument(): void  :108
- [   ] readsNumberFormatFromNumberingXml(): void  :118
- [   ] survivesMissingNumberingDefinition(): void  :128
- [   ] skipsEmptyParagraphs(): void  :137
- [prv] convert(org.apache.poi.xwpf.usermodel.XWPFDocument doc): String  :144
- [prv] count(String haystack, String needle): int  :148

### EditorBlotSanitizationTest  (src/test/java/pl/szymtrener/content/EditorBlotSanitizationTest.java)
- [   ] video(): void  :49
- [   ] pdf(): void  :58
- [   ] table(): void  :71
- [   ] videoKeepsItsLook(): void  :87
- [   ] pdfKeepsItsLook(): void  :98
- [   ] chromeIsRemovedWithItsText(): void  :123
- [   ] figureItselfSurvives(): void  :137
- [   ] svgCannotCarryScripts(): void  :153
- [   ] linkClassDoesNotOpenJavascriptProtocol(): void  :169
- [   ] iframeIsStillBlocked(): void  :179

### EditorHtml  (src/main/java/pl/szymtrener/content/EditorHtml.java)
- [pub] toPublication(String html): String  :33
- [prv] figureToPublication(Element figure): void  :50
- [prv] videoToPublication(Element block): void  :71
- [prv] pdfToPublication(Element block): void  :93
- [pub] toEditor(String publicationHtml): String  :131
- [prv] figureToEditor(Element figure): void  :144
- [prv] videoToEditor(Element block): void  :156
- [prv] pdfToEditor(Element block): void  :163
- [pub] requireImageAlts(String publicationHtml): void  :183
- [prv] firstNonBlank(String first, String second): String  :195

### EditorHtmlTest  (src/test/java/pl/szymtrener/content/EditorHtmlTest.java)
- [prv] publish(String editorHtml): String  :48
- [   ] editorClassesAreGone(): void  :58
- [   ] figure(): void  :79
- [   ] video(): void  :92
- [   ] pdf(): void  :104
- [   ] plainContentSurvives(): void  :115
- [prv] reopen(String editorHtml): String  :129
- [   ] figureSurvivesRoundTrip(): void  :135
- [   ] videoSurvivesRoundTrip(): void  :146
- [   ] pdfSurvivesRoundTrip(): void  :156
- [   ] secondSaveIsIdempotent(): void  :168
- [   ] missingAltIsRejected(): void  :182
- [   ] videoThumbnailIsExempt(): void  :193
- [   ] completeAltsPass(): void  :199

### GlobalExceptionHandler  (src/main/java/pl/szymtrener/common/GlobalExceptionHandler.java)
- [pub] handle(Exception exception, HttpServletRequest request): Object  :39
- [prv] wantsJson(HttpServletRequest request): boolean  :63

### GlobalExceptionHandlerTest  (src/test/java/pl/szymtrener/common/GlobalExceptionHandlerTest.java)
- [   ] rethrowsMissingStaticResource(): void  :25
- [   ] rethrowsAnnotatedException(): void  :34
- [   ] handlesRealFailure(): void  :43

### HomeController  (src/main/java/pl/szymtrener/web/HomeController.java)
- [pub] home(Model model): String  :28
- [pub] privacy(Model model): String  :43

### HtmlSanitizer  (src/main/java/pl/szymtrener/content/HtmlSanitizer.java)
- [pub] clean(String html): String  :52

### HtmlSanitizerTest  (src/test/java/pl/szymtrener/HtmlSanitizerTest.java)
- [   ] usuwaSkryptyIStyleZWklejonejTresci(): void  :13
- [   ] zachowujeTabeleZNaglowkami(): void  :22
- [   ] zachowujeBlokFilmuZEdytora(): void  :31
- [   ] dodajeLazyLoadingObrazkom(): void  :39

### IndexNowService  (src/main/java/pl/szymtrener/seo/IndexNowService.java)
- [pub] submit(List<String> paths): void  :33

### JsonLdService  (src/main/java/pl/szymtrener/seo/JsonLdService.java)
- [pub] forBlogList(String canonical, List<PostView> posts): List<String>  :26
- [pub] forPost(Post post, PostView view, String canonical): List<String>  :50
- [prv] authorNode(Post post): Map<String, Object>  :99
- [prv] person(String name): Map<String, Object>  :111
- [prv] organization(): Map<String, Object>  :115
- [prv] breadcrumbs(List<Map.Entry<String, String>> trail): Map<String, Object>  :125
- [prv] stripTags(String html): String  :143
- [prv] write(Object node): String  :147

### LegacyDocConverter  (src/main/java/pl/szymtrener/docimport/LegacyDocConverter.java)
- [pub] convert(InputStream in): ImportResult  :22

### MailConfig  (src/main/java/pl/szymtrener/config/MailConfig.java)
- [   ] reportConfiguration(): void  :46
- [prv] mask(String address): String  :81

### MailService  (src/main/java/pl/szymtrener/submission/MailService.java)
- [pub] sendNotifications(Submission s): void  :35
- [prv] trainerNotification(Submission s): SimpleMailMessage  :53
- [prv] autoReply(Submission s): SimpleMailMessage  :63
- [prv] body(Submission s): String  :83
- [prv] line(StringBuilder sb, String label, String value): void  :100

### MediaBlob  (src/main/java/pl/szymtrener/media/MediaBlob.java)
- [pub] getMediaId(): Long  :23
- [pub] getData(): byte[]  :24

### MediaController  (src/main/java/pl/szymtrener/media/MediaController.java)
- [pub] serve(HttpServletRequest request): ResponseEntity<byte[]>  :21
- [pub] download(@PathVariable Long id): ResponseEntity<byte[]>  :37

### MediaFile  (src/main/java/pl/szymtrener/media/MediaFile.java)
- [pub] getId(): Long  :47
- [pub] getStorageKey(): String  :48
- [pub] setStorageKey(String storageKey): void  :49
- [pub] getOriginalName(): String  :50
- [pub] setOriginalName(String originalName): void  :51
- [pub] getMimeType(): String  :52
- [pub] setMimeType(String mimeType): void  :53
- [pub] getKind(): MediaKind  :54
- [pub] setKind(MediaKind kind): void  :55
- [pub] getSizeBytes(): long  :56
- [pub] setSizeBytes(long sizeBytes): void  :57
- [pub] getWidth(): Integer  :58
- [pub] setWidth(Integer width): void  :59
- [pub] getHeight(): Integer  :60
- [pub] setHeight(Integer height): void  :61
- [pub] getPageCount(): Integer  :62
- [pub] setPageCount(Integer pageCount): void  :63
- [pub] getAltText(): String  :64
- [pub] setAltText(String altText): void  :65
- [pub] getTitle(): String  :66
- [pub] setTitle(String title): void  :67
- [pub] getChecksum(): String  :68
- [pub] setChecksum(String checksum): void  :69
- [pub] getDownloadCount(): long  :70
- [pub] setDownloadCount(long downloadCount): void  :71
- [pub] getCreatedAt(): Instant  :72
- [pub] publicUrl(): String  :74
- [pub] humanSize(): String  :78

### MediaRepository  (src/main/java/pl/szymtrener/media/MediaRepository.java)
- [   ] findByStorageKey(String storageKey): Optional<MediaFile>  :10
- [   ] findByChecksum(String checksum): Optional<MediaFile>  :11
- [   ] findAllByOrderByCreatedAtDesc(Pageable pageable): Page<MediaFile>  :12
- [   ] findByKindOrderByCreatedAtDesc(MediaKind kind, Pageable pageable): Page<MediaFile>  :13
- [   ] countByKind(MediaKind kind): long  :15
- [   ] totalBytes(): long  :19

### MediaService  (src/main/java/pl/szymtrener/media/MediaService.java)
- [pub] upload(MultipartFile file, String altText): MediaFile  :39
- [pub] store(byte[] bytes, String originalName, String mime, String altText): MediaFile  :98
- [pub] bytes(Long mediaId): Optional<byte[]>  :132
- [pub] byStorageKey(String key): Optional<MediaFile>  :137
- [pub] byId(Long id): Optional<MediaFile>  :142
- [pub] countDownload(Long id): void  :147
- [pub] delete(Long id): void  :156
- [pub] publicUrl(Long mediaId): String  :163
- [prv] extensionFor(String mime): String  :169
- [prv] safeName(String name): String  :179
- [prv] sha256(byte[] data): String  :184

### PageView  (src/main/java/pl/szymtrener/analytics/PageView.java)
- [pub] getId(): Long  :23
- [pub] getPath(): String  :24
- [pub] setPath(String path): void  :25
- [pub] getReferrer(): String  :26
- [pub] setReferrer(String referrer): void  :27
- [pub] getSessionHash(): String  :28
- [pub] setSessionHash(String sessionHash): void  :29
- [pub] getDevice(): String  :30
- [pub] setDevice(String device): void  :31
- [pub] isBot(): boolean  :32
- [pub] setBot(boolean bot): void  :33
- [pub] getBotName(): String  :34
- [pub] setBotName(String botName): void  :35
- [pub] getViewedAt(): Instant  :36

### PasswordForm  (src/main/java/pl/szymtrener/admin/PasswordForm.java)
- [pub] getCurrent(): String  :8
- [pub] setCurrent(String current): void  :9
- [pub] getFresh(): String  :10
- [pub] setFresh(String fresh): void  :11
- [pub] getRepeat(): String  :12
- [pub] setRepeat(String repeat): void  :13

### Post  (src/main/java/pl/szymtrener/content/Post.java)
- [   ] touch(): void  :108
- [pub] isPublished(): boolean  :110
- [pub] dateLabel(): String  :118
- [pub] addFaq(PostFaq item): void  :133
- [pub] getId(): Long  :135
- [pub] getSlug(): String  :136
- [pub] setSlug(String slug): void  :137
- [pub] getTitle(): String  :138
- [pub] setTitle(String title): void  :139
- [pub] getLead(): String  :140
- [pub] setLead(String lead): void  :141
- [pub] getContentHtml(): String  :142
- [pub] setContentHtml(String contentHtml): void  :143
- [pub] getContentDelta(): String  :144
- [pub] setContentDelta(String contentDelta): void  :145
- [pub] getCategory(): Category  :146
- [pub] setCategory(Category category): void  :147
- [pub] getAuthor(): Author  :148
- [pub] setAuthor(Author author): void  :149
- [pub] getCoverMediaId(): Long  :150
- [pub] setCoverMediaId(Long coverMediaId): void  :151
- [pub] getCoverAlt(): String  :152
- [pub] setCoverAlt(String coverAlt): void  :153
- [pub] getCoverCaption(): String  :154
- [pub] setCoverCaption(String coverCaption): void  :155
- [pub] getStatus(): PostStatus  :156
- [pub] setStatus(PostStatus status): void  :157
- [pub] getPublishAt(): Instant  :158
- [pub] setPublishAt(Instant publishAt): void  :159
- [pub] getPublishedAt(): Instant  :160
- [pub] setPublishedAt(Instant publishedAt): void  :161
- [pub] getSeoTitle(): String  :162
- [pub] setSeoTitle(String seoTitle): void  :163
- [pub] getSeoDescription(): String  :164
- [pub] setSeoDescription(String seoDescription): void  :165
- [pub] getReadingMinutes(): int  :166
- [pub] setReadingMinutes(int readingMinutes): void  :167
- [pub] getWordCount(): int  :168
- [pub] setWordCount(int wordCount): void  :169
- [pub] getViewCount(): long  :170
- [pub] setViewCount(long viewCount): void  :171
- [pub] getAiScore(): Integer  :172
- [pub] setAiScore(Integer aiScore): void  :173
- [pub] isHasVideo(): boolean  :174
- [pub] setHasVideo(boolean hasVideo): void  :175
- [pub] isHasPdf(): boolean  :176
- [pub] setHasPdf(boolean hasPdf): void  :177
- [pub] getCreatedAt(): Instant  :178
- [pub] getUpdatedAt(): Instant  :179
- [pub] setUpdatedAt(Instant updatedAt): void  :180
- [pub] getTags(): Set<String>  :181
- [pub] getSummaryPoints(): List<String>  :182
- [pub] getFaq(): List<PostFaq>  :183

### PostFaq  (src/main/java/pl/szymtrener/content/PostFaq.java)
- [pub] getId(): Long  :25
- [pub] getPost(): Post  :26
- [pub] setPost(Post post): void  :27
- [pub] getPosition(): int  :28
- [pub] setPosition(int position): void  :29
- [pub] getQuestion(): String  :30
- [pub] setQuestion(String question): void  :31
- [pub] getAnswer(): String  :32
- [pub] setAnswer(String answer): void  :33

### PostFlowIT  (src/test/java/pl/szymtrener/content/PostFlowIT.java)
- [   ] clean(): void  :34
- [prv] published(String title, String html): Post  :44
- [prv] published(String title, String html, String categorySlug): Post  :48
- [   ] oldSlugRedirectsPermanently(): void  :62
- [   ] draftSlugChangeLeavesNoHistory(): void  :82
- [   ] revertingTitleDoesNotCreateSelfRedirect(): void  :97
- [   ] unknownSlugStillReturns404(): void  :115
- [   ] searchFindsByTitle(): void  :124
- [   ] emptyQueryReturnsNothing(): void  :136
- [   ] searchSkipsDrafts(): void  :145
- [   ] relatedFillsInsteadOfReplacing(): void  :159
- [   ] savingPostRecordsMediaLinks(): void  :178
- [   ] removingImageDropsTheLink(): void  :195
- [   ] publishedPostIsArchivedNotDeleted(): void  :216
- [   ] draftIsDeleted(): void  :228
- [   ] duplicateTitlesGetDistinctSlugs(): void  :245
- [prv] insertMedia(String storageKey): Long  :253

### PostForm  (src/main/java/pl/szymtrener/admin/PostForm.java)
- [pub] getId(): Long  :27
- [pub] setId(Long id): void  :28
- [pub] getTitle(): String  :29
- [pub] setTitle(String title): void  :30
- [pub] getSlug(): String  :31
- [pub] setSlug(String slug): void  :32
- [pub] getLead(): String  :33
- [pub] setLead(String lead): void  :34
- [pub] getContentHtml(): String  :35
- [pub] setContentHtml(String contentHtml): void  :36
- [pub] getContentDelta(): String  :37
- [pub] setContentDelta(String contentDelta): void  :38
- [pub] getCategoryId(): Long  :39
- [pub] setCategoryId(Long categoryId): void  :40
- [pub] getCoverMediaId(): Long  :41
- [pub] setCoverMediaId(Long coverMediaId): void  :42
- [pub] getCoverAlt(): String  :43
- [pub] setCoverAlt(String coverAlt): void  :44
- [pub] getCoverCaption(): String  :45
- [pub] setCoverCaption(String coverCaption): void  :46
- [pub] getStatus(): String  :47
- [pub] setStatus(String status): void  :48
- [pub] getPublishAt(): String  :49
- [pub] setPublishAt(String publishAt): void  :50
- [pub] getTags(): String  :51
- [pub] setTags(String tags): void  :52
- [pub] getSeoTitle(): String  :53
- [pub] setSeoTitle(String seoTitle): void  :54
- [pub] getSeoDescription(): String  :55
- [pub] setSeoDescription(String seoDescription): void  :56
- [pub] getSummaryPoints(): List<String>  :57
- [pub] setSummaryPoints(List<String> summaryPoints): void  :58
- [pub] getFaqQuestions(): List<String>  :59
- [pub] setFaqQuestions(List<String> faqQuestions): void  :60
- [pub] getFaqAnswers(): List<String>  :61
- [pub] setFaqAnswers(List<String> faqAnswers): void  :62

### PostMedia  (src/main/java/pl/szymtrener/content/PostMedia.java)
- [pub] getPostId(): Long  :37
- [pub] getMediaId(): Long  :38
- [pub] getRole(): String  :39

### PostMediaRepository  (src/main/java/pl/szymtrener/content/PostMediaRepository.java)
- [   ] deleteByPostId(Long postId): void  :13
- [   ] existsByMediaId(Long mediaId): boolean  :15

### PostPageModel  (src/main/java/pl/szymtrener/web/PostPageModel.java)
- [pub] fill(Long postId, Model model, boolean preview): String  :49

### PostRepository  (src/main/java/pl/szymtrener/content/PostRepository.java)
- [   ] findBySlug(String slug): Optional<Post>  :13
- [   ] findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable): Page<Post>  :16
- [   ] findByStatusAndCategorySlugOrderByPublishedAtDesc(PostStatus status, String categorySlug, Pageable pageable): Page<Post>  :19
- [   ] findBySlugAndStatus(String slug, PostStatus status): Optional<Post>  :22
- [   ] findByStatusAndPublishAtLessThanEqual(PostStatus status, Instant now): List<Post>  :28
- [   ] findTop3ByStatusAndCategoryIdAndIdNotOrderByPublishedAtDesc(PostStatus status, Long categoryId, Long excludedId): List<Post>  :31
- [   ] findTop3ByStatusAndIdNotOrderByPublishedAtDesc(PostStatus status, Long excludedId): List<Post>  :34
- [   ] findTop6ByStatusAndIdNotOrderByPublishedAtDesc(PostStatus status, Long excludedId): List<Post>  :38
- [   ] findSlugsForSitemap(): List<Object[]>  :59
- [   ] findTop20ByStatusOrderByPublishedAtDesc(PostStatus status): List<Post>  :63
- [   ] countByStatus(PostStatus status): long  :69
- [   ] findAllByOrderByUpdatedAtDesc(Pageable pageable): Page<Post>  :75
- [   ] findByStatusOrderByUpdatedAtDesc(PostStatus status, Pageable pageable): Page<Post>  :89
- [   ] findTop5ByStatusOrderByPublishAtAsc(PostStatus status): List<Post>  :107
- [   ] findTop5ByStatusOrderByUpdatedAtDesc(PostStatus status): List<Post>  :111

### PostService  (src/main/java/pl/szymtrener/content/PostService.java)
- [pub] published(String categorySlug, int page, int size): Page<PostView>  :56
- [pub] requirePublished(String slug): Post  :65
- [pub] related(Post post): List<PostView>  :76
- [pub] search(String query, int page, int size): Page<PostView>  :96
- [pub] registerView(Long postId): void  :104
- [pub] toFullView(Post p): PostView  :110
- [pub] toCardView(Post p): PostView  :132
- [pub] save(Post post): Post  :149
- [pub] save(Post post, String desiredSlug): Post  :159
- [prv] syncMediaLinks(Post post): void  :201
- [prv] mediaIdFromUrl(String url): Optional<Long>  :231
- [pub] uniqueSlug(String base, Long selfId): String  :253
- [pub] postsUsing(Long mediaId): List<String>  :268
- [pub] deleteOrArchive(Post post): boolean  :279
- [pub] categories(): List<Category>  :291
- [   ] iso(Instant instant): String  :293
- [   ] label(Instant instant): String  :297

### PostSlugHistory  (src/main/java/pl/szymtrener/content/PostSlugHistory.java)
- [pub] getSlug(): String  :28
- [pub] getPostId(): Long  :29
- [pub] getCreatedAt(): Instant  :30

### PostSlugHistoryRepository  (src/main/java/pl/szymtrener/content/PostSlugHistoryRepository.java)
- [   ] findBySlug(String slug): Optional<PostSlugHistory>  :8
- [   ] deleteBySlug(String slug): void  :9

### PostStatus  (src/main/java/pl/szymtrener/content/PostStatus.java)
- [pub] label(): String  :19
- [pub] badge(): String  :22

### PostgresTestBase  (src/test/java/pl/szymtrener/PostgresTestBase.java)
- [   ] datasource(DynamicPropertyRegistry registry): void  :46
- [prv] constant(String value): Supplier<Object>  :58
- [prv] property(String systemProperty, String environmentVariable): String  :62

### PublicFormController  (src/main/java/pl/szymtrener/web/PublicFormController.java)
- [pub] online(@Valid @RequestBody FormRequests.OnlineForm form, BindingResult errors, HttpServletRequest request): ResponseEntity<?>  :29
- [pub] contact(@Valid @RequestBody FormRequests.ContactForm form, BindingResult errors, HttpServletRequest request): ResponseEntity<?>  :38
- [prv] reject(BindingResult errors, Boolean honeypot, HttpServletRequest request): ResponseEntity<?>  :46
- [prv] ip(HttpServletRequest request): String  :63

### PublicFormControllerTest  (src/test/java/pl/szymtrener/web/PublicFormControllerTest.java)
- [   ] allowByDefault(): void  :60
- [prv] validContact(): Map<String, Object>  :64
- [prv] validOnline(): Map<String, Object>  :76
- [   ] acceptsValidContactForm(): void  :93
- [   ] acceptsValidOnlineForm(): void  :105
- [   ] rejectsRequestWithoutCsrfToken(): void  :117
- [   ] honeypotSilentlyDropsBotSubmissions(): void  :128
- [   ] honeypotWinsOverValidation(): void  :144
- [   ] rejectsWhenRateLimited(): void  :160
- [   ] requiresConsent(): void  :175
- [   ] reportsFieldValidationErrors(): void  :191
- [   ] onlineFormRequiresItsOwnFields(): void  :208
- [   ] rateLimitIsCheckedBeforeValidation(): void  :225

### PublishScheduler  (src/main/java/pl/szymtrener/scheduler/PublishScheduler.java)
- [pub] publishDue(): void  :32

### RateLimiter  (src/main/java/pl/szymtrener/submission/RateLimiter.java)
- [pub] allow(String key): boolean  :24

### SecurityConfig  (src/main/java/pl/szymtrener/config/SecurityConfig.java)
- [   ] filterChain(HttpSecurity http): SecurityFilterChain  :15
- [   ] passwordEncoder(): PasswordEncoder  :41

### SeoController  (src/main/java/pl/szymtrener/seo/SeoController.java)
- [pub] robots(): String  :44
- [pub] sitemap(): String  :90
- [prv] url(StringBuilder xml, String location, Instant lastMod, String priority): void  :105
- [pub] feed(): String  :119
- [prv] rfc822(Instant instant): String  :158
- [prv] escape(String text): String  :162
- [pub] indexNowKey(@PathVariable String key): String  :169
- [pub] llms(): String  :179

### SeoScoreService  (src/main/java/pl/szymtrener/seo/SeoScoreService.java)
- [pub] pending(): List<Check>  :41
- [pub] evaluate(String contentHtml, String seoTitle, String seoDesc, String coverAlt): Result  :52
- [pub] ofPublication(String publicationHtml, String seoTitle, String seoDesc, String coverAlt): Result  :91
- [prv] hasImage(Document doc): boolean  :96
- [prv] allImagesHaveAlt(Document doc): boolean  :104
- [prv] label(int score): String  :110
- [prv] hint(long done, int total): String  :116
- [prv] trim(String value): String  :121

### SettingsService  (src/main/java/pl/szymtrener/settings/SettingsService.java)
- [pub] get(String key, String fallback): String  :39
- [pub] getInt(String key, int fallback): int  :45
- [pub] getBoolean(String key, boolean fallback): boolean  :56
- [pub] set(String key, String value): void  :62
- [pub] all(): Map<String, String>  :72
- [prv] ensureLoaded(): void  :77

### SlugUtil  (src/main/java/pl/szymtrener/common/SlugUtil.java)
- [pub] slugify(String input): String  :10

### SlugUtilTest  (src/test/java/pl/szymtrener/common/SlugUtilTest.java)
- [   ] slugifiesPolishText(String input, String expected): void  :26
- [   ] handlesEmptyInput(String input): void  :34
- [   ] trimsLongTitles(): void  :40
- [   ] handlesPunctuationOnlyTitle(): void  :53
- [   ] isDeterministic(): void  :59

### Submission  (src/main/java/pl/szymtrener/submission/Submission.java)
- [pub] getId(): Long  :39
- [pub] getType(): SubmissionType  :40
- [pub] setType(SubmissionType type): void  :41
- [pub] getName(): String  :42
- [pub] setName(String name): void  :43
- [pub] getEmail(): String  :44
- [pub] setEmail(String email): void  :45
- [pub] getPhone(): String  :46
- [pub] setPhone(String phone): void  :47
- [pub] getCity(): String  :48
- [pub] setCity(String city): void  :49
- [pub] getCurrentTraining(): String  :50
- [pub] setCurrentTraining(String currentTraining): void  :51
- [pub] getGoal(): String  :52
- [pub] setGoal(String goal): void  :53
- [pub] getEquipment(): String  :54
- [pub] setEquipment(String equipment): void  :55
- [pub] getSource(): String  :56
- [pub] setSource(String source): void  :57
- [pub] getInterest(): String  :58
- [pub] setInterest(String interest): void  :59
- [pub] getMessage(): String  :60
- [pub] setMessage(String message): void  :61
- [pub] getConsentAt(): Instant  :62
- [pub] setConsentAt(Instant consentAt): void  :63
- [pub] getStatus(): SubmissionStatus  :64
- [pub] setStatus(SubmissionStatus status): void  :65
- [pub] getCallAt(): Instant  :66
- [pub] setCallAt(Instant callAt): void  :67
- [pub] getIpHash(): String  :68
- [pub] setIpHash(String ipHash): void  :69
- [pub] getUserAgent(): String  :70
- [pub] setUserAgent(String userAgent): void  :71
- [pub] isMailSent(): boolean  :72
- [pub] setMailSent(boolean mailSent): void  :73
- [pub] getMailError(): String  :74
- [pub] setMailError(String mailError): void  :75
- [pub] getCreatedAt(): Instant  :76
- [pub] callAtLocal(): String  :80
- [pub] initials(): String  :88
- [pub] getCreatedLabel(): String  :99

### SubmissionNote  (src/main/java/pl/szymtrener/submission/SubmissionNote.java)
- [pub] getId(): Long  :19
- [pub] getSubmissionId(): Long  :20
- [pub] setSubmissionId(Long submissionId): void  :21
- [pub] getAuthor(): String  :22
- [pub] setAuthor(String author): void  :23
- [pub] getBody(): String  :24
- [pub] setBody(String body): void  :25
- [pub] getCreatedAt(): Instant  :26
- [pub] getCreatedLabel(): String  :29

### SubmissionNoteRepository  (src/main/java/pl/szymtrener/submission/SubmissionNoteRepository.java)
- [   ] findBySubmissionIdOrderByCreatedAtDesc(Long submissionId): List<SubmissionNote>  :7

### SubmissionRepository  (src/main/java/pl/szymtrener/submission/SubmissionRepository.java)
- [   ] findAllByOrderByCreatedAtDesc(Pageable pageable): Page<Submission>  :10
- [   ] findByStatusOrderByCreatedAtDesc(SubmissionStatus status, Pageable pageable): Page<Submission>  :11
- [   ] findByTypeOrderByCreatedAtDesc(SubmissionType type, Pageable pageable): Page<Submission>  :12
- [   ] findTop5ByOrderByCreatedAtDesc(): List<Submission>  :13
- [   ] countByStatus(SubmissionStatus status): long  :14
- [   ] countByCreatedAtAfter(Instant since): long  :15

### SubmissionService  (src/main/java/pl/szymtrener/submission/SubmissionService.java)
- [pub] acceptOnline(FormRequests.OnlineForm form, String ip, String userAgent): Submission  :30
- [pub] acceptContact(FormRequests.ContactForm form, String ip, String userAgent): Submission  :45
- [prv] persist(Submission s, String ip, String userAgent): Submission  :56
- [pub] changeStatus(Long id, SubmissionStatus status, Instant callAt): void  :66
- [pub] delete(Long id): void  :78
- [pub] export(Long id): Map<String, Object>  :87
- [pub] addNote(Long submissionId, String author, String body): void  :122
- [prv] hash(String ip): String  :130

### SubmissionStatus  (src/main/java/pl/szymtrener/submission/SubmissionStatus.java)
- [pub] label(): String  :6
- [pub] badge(): String  :17

### SubmissionType  (src/main/java/pl/szymtrener/submission/SubmissionType.java)
- [pub] label(): String  :13

### SzymtrenerApplication  (src/main/java/pl/szymtrener/SzymtrenerApplication.java)
- [pub] main(String[] args): void  :14

### Trainee  (src/main/java/pl/szymtrener/crm/Trainee.java)
- [pub] getId(): Long  :45
- [pub] getSubmissionId(): Long  :46
- [pub] setSubmissionId(Long submissionId): void  :47
- [pub] getName(): String  :48
- [pub] setName(String name): void  :49
- [pub] getCity(): String  :50
- [pub] setCity(String city): void  :51
- [pub] getAge(): Integer  :52
- [pub] setAge(Integer age): void  :53
- [pub] getMode(): TraineeMode  :54
- [pub] setMode(TraineeMode mode): void  :55
- [pub] getStartedAt(): LocalDate  :56
- [pub] setStartedAt(LocalDate startedAt): void  :57
- [pub] getPlanName(): String  :58
- [pub] setPlanName(String planName): void  :59
- [pub] getSessionCount(): int  :60
- [pub] setSessionCount(int sessionCount): void  :61
- [pub] getStatus(): TraineeStatus  :62
- [pub] setStatus(TraineeStatus status): void  :63
- [pub] getCreatedAt(): Instant  :64

### TraineeForm  (src/main/java/pl/szymtrener/crm/TraineeForm.java)
- [pub] getId(): Long  :17
- [pub] setId(Long id): void  :18
- [pub] getSubmissionId(): Long  :19
- [pub] setSubmissionId(Long submissionId): void  :20
- [pub] getName(): String  :21
- [pub] setName(String name): void  :22
- [pub] getCity(): String  :23
- [pub] setCity(String city): void  :24
- [pub] getAge(): Integer  :25
- [pub] setAge(Integer age): void  :26
- [pub] getMode(): TraineeMode  :27
- [pub] setMode(TraineeMode mode): void  :28
- [pub] getStartedAt(): LocalDate  :29
- [pub] setStartedAt(LocalDate startedAt): void  :30
- [pub] getPlanName(): String  :31
- [pub] setPlanName(String planName): void  :32
- [pub] getSessionCount(): int  :33
- [pub] setSessionCount(int sessionCount): void  :34
- [pub] getStatus(): TraineeStatus  :35
- [pub] setStatus(TraineeStatus status): void  :36

### TraineeMode  (src/main/java/pl/szymtrener/crm/TraineeMode.java)
- [pub] label(): String  :6

### TraineeRepository  (src/main/java/pl/szymtrener/crm/TraineeRepository.java)
- [   ] findAllOrdered(Pageable pageable): Page<Trainee>  :18
- [   ] countByStatus(TraineeStatus status): long  :23
- [   ] findBySubmissionId(Long submissionId): Optional<Trainee>  :25
- [   ] countByMode(TraineeMode mode): long  :27

### TraineeService  (src/main/java/pl/szymtrener/crm/TraineeService.java)
- [pub] fromSubmission(Long submissionId): Trainee  :36
- [pub] save(TraineeForm form): Trainee  :59
- [pub] delete(Long id): void  :78
- [prv] blankToNull(String value): String  :82

### TraineeStatus  (src/main/java/pl/szymtrener/crm/TraineeStatus.java)
- [pub] label(): String  :6
- [pub] badge(): String  :15

