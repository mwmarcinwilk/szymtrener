# INVENTORY — szymtrener
> AUTO-GENEROWANY 2026-11-13 przez tools/index_project.py — NIE edytuj ręcznie.
> To spis tego, co JUŻ ISTNIEJE. Grepuj go zanim zbudujesz coś nowego.
> Beany: 65 · Endpointy: 100 · Metody: 1406 · Front: 0

## Beany / komponenty Spring
@Component      AdminNav — src/main/java/pl/szymtrener/admin/AdminNav.java
@Component      AnalyticsFilter — src/main/java/pl/szymtrener/analytics/AnalyticsFilter.java
@Component      AnalyticsView — src/main/java/pl/szymtrener/analytics/AnalyticsView.java
@Component      AttachmentLimits — src/main/java/pl/szymtrener/crm/AttachmentLimits.java
@Component      CleanupScheduler — src/main/java/pl/szymtrener/scheduler/CleanupScheduler.java
@Component      ConsentView — src/main/java/pl/szymtrener/consent/ConsentView.java
@Component      ConsentedPageViews — src/main/java/pl/szymtrener/analytics/ConsentedPageViews.java
@Component      ContentMetrics — src/main/java/pl/szymtrener/content/ContentMetrics.java
@Component      DatabaseUrlEnvironmentPostProcessor — src/main/java/pl/szymtrener/config/DatabaseUrlEnvironmentPostProcessor.java
@Component      DocxToHtmlConverter — src/main/java/pl/szymtrener/docimport/DocxToHtmlConverter.java
@Component      EditorHtml — src/main/java/pl/szymtrener/content/EditorHtml.java
@Component      HtmlSanitizer — src/main/java/pl/szymtrener/content/HtmlSanitizer.java
@Component      HttpThumbnailFetcher — src/main/java/pl/szymtrener/media/HttpThumbnailFetcher.java
@Component      InboundMailScheduler — src/main/java/pl/szymtrener/scheduler/InboundMailScheduler.java
@Component      InboundMatcher — src/main/java/pl/szymtrener/crm/InboundMatcher.java
@Component      LegacyDocConverter — src/main/java/pl/szymtrener/docimport/LegacyDocConverter.java
@Component      LoginAuditListener — src/main/java/pl/szymtrener/admin/LoginAuditListener.java
@Component      PostPageModel — src/main/java/pl/szymtrener/web/PostPageModel.java
@Component      PublishScheduler — src/main/java/pl/szymtrener/scheduler/PublishScheduler.java
@Component      RateLimiter — src/main/java/pl/szymtrener/submission/RateLimiter.java
@Component      ReminderScheduler — src/main/java/pl/szymtrener/submission/ReminderScheduler.java
@Component      YoutubeThumbnailBackfill — src/main/java/pl/szymtrener/content/YoutubeThumbnailBackfill.java
@Configuration  AdminAccountInitializer — src/main/java/pl/szymtrener/admin/AdminAccountInitializer.java
@Configuration  MailConfig — src/main/java/pl/szymtrener/config/MailConfig.java
@Configuration  SecurityConfig — src/main/java/pl/szymtrener/config/SecurityConfig.java
@Configuration  WebConfig — src/main/java/pl/szymtrener/config/WebConfig.java
@Controller     AdminAttachmentController — src/main/java/pl/szymtrener/admin/AdminAttachmentController.java
@Controller     AdminConsentController — src/main/java/pl/szymtrener/admin/AdminConsentController.java
@Controller     AdminController — src/main/java/pl/szymtrener/admin/AdminController.java
@Controller     AdminMediaController — src/main/java/pl/szymtrener/admin/AdminMediaController.java
@Controller     AdminOfferController — src/main/java/pl/szymtrener/admin/AdminOfferController.java
@Controller     AdminPostController — src/main/java/pl/szymtrener/admin/AdminPostController.java
@Controller     AdminSettingsController — src/main/java/pl/szymtrener/admin/AdminSettingsController.java
@Controller     AdminStatsController — src/main/java/pl/szymtrener/admin/AdminStatsController.java
@Controller     AdminSubmissionController — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
@Controller     AdminTraineeController — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
@Controller     AdminUsersController — src/main/java/pl/szymtrener/admin/AdminUsersController.java
@Controller     BlogController — src/main/java/pl/szymtrener/web/BlogController.java
@Controller     ConsentController — src/main/java/pl/szymtrener/web/ConsentController.java
@Controller     HomeController — src/main/java/pl/szymtrener/web/HomeController.java
@Controller     NoteController — src/main/java/pl/szymtrener/admin/NoteController.java
@RestController AdminApiController — src/main/java/pl/szymtrener/admin/AdminApiController.java
@RestController MediaController — src/main/java/pl/szymtrener/media/MediaController.java
@RestController PublicFormController — src/main/java/pl/szymtrener/web/PublicFormController.java
@RestController SeoController — src/main/java/pl/szymtrener/seo/SeoController.java
@Service        AdminUserDetailsService — src/main/java/pl/szymtrener/admin/AdminUserDetailsService.java
@Service        AiReadinessAnalyzer — src/main/java/pl/szymtrener/seo/AiReadinessAnalyzer.java
@Service        AttachmentService — src/main/java/pl/szymtrener/crm/AttachmentService.java
@Service        ClientInsightService — src/main/java/pl/szymtrener/crm/ClientInsightService.java
@Service        ConsentService — src/main/java/pl/szymtrener/consent/ConsentService.java
@Service        DocImportService — src/main/java/pl/szymtrener/docimport/DocImportService.java
@Service        InboundMailService — src/main/java/pl/szymtrener/crm/InboundMailService.java
@Service        IndexNowService — src/main/java/pl/szymtrener/seo/IndexNowService.java
@Service        JsonLdService — src/main/java/pl/szymtrener/seo/JsonLdService.java
@Service        MailService — src/main/java/pl/szymtrener/submission/MailService.java
@Service        MediaService — src/main/java/pl/szymtrener/media/MediaService.java
@Service        MessageService — src/main/java/pl/szymtrener/crm/MessageService.java
@Service        OnlineOfferService — src/main/java/pl/szymtrener/offer/OnlineOfferService.java
@Service        PostService — src/main/java/pl/szymtrener/content/PostService.java
@Service        SeoScoreService — src/main/java/pl/szymtrener/seo/SeoScoreService.java
@Service        SettingsService — src/main/java/pl/szymtrener/settings/SettingsService.java
@Service        StationaryOfferService — src/main/java/pl/szymtrener/offer/StationaryOfferService.java
@Service        SubmissionService — src/main/java/pl/szymtrener/submission/SubmissionService.java
@Service        TraineeService — src/main/java/pl/szymtrener/crm/TraineeService.java
@Service        YoutubeThumbnails — src/main/java/pl/szymtrener/media/YoutubeThumbnails.java

## Endpointy REST
GET     /                                             HomeController.home() — src/main/java/pl/szymtrener/web/HomeController.java
GET     /admin                                        AdminController.dashboard() — src/main/java/pl/szymtrener/admin/AdminController.java
GET     /admin/administratorzy                        AdminUsersController.list() — src/main/java/pl/szymtrener/admin/AdminUsersController.java
GET     /admin/administratorzy/{id}                   AdminUsersController.edit() — src/main/java/pl/szymtrener/admin/AdminUsersController.java
GET     /admin/api/media                              AdminApiController.list() — src/main/java/pl/szymtrener/admin/AdminApiController.java
GET     /admin/haslo                                  AdminController.passwordForm() — src/main/java/pl/szymtrener/admin/AdminController.java
GET     /admin/klienci                                AdminTraineeController.list() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
GET     /admin/klienci/nowy                           AdminTraineeController.create() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
GET     /admin/klienci/tydzien                        AdminTraineeController.week() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
GET     /admin/klienci/{id}                           AdminTraineeController.profile() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
GET     /admin/klienci/{id}/edycja                    AdminTraineeController.edit() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
GET     /admin/logowanie                              AdminController.login() — src/main/java/pl/szymtrener/admin/AdminController.java
GET     /admin/media                                  AdminMediaController.library() — src/main/java/pl/szymtrener/admin/AdminMediaController.java
GET     /admin/oferta                                 AdminOfferController.overview() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
GET     /admin/oferta/opinie/{id}                     AdminOfferController.editTestimonial() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
GET     /admin/oferta/pakiety/{id}                    AdminOfferController.editPackage() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
GET     /admin/oferta/stacjonarnie                    AdminOfferController.stationary() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
GET     /admin/posty                                  AdminPostController.list() — src/main/java/pl/szymtrener/admin/AdminPostController.java
GET     /admin/posty/nowy                             AdminPostController.create() — src/main/java/pl/szymtrener/admin/AdminPostController.java
GET     /admin/posty/{id}                             AdminPostController.edit() — src/main/java/pl/szymtrener/admin/AdminPostController.java
GET     /admin/posty/{id}/podglad                     AdminPostController.preview() — src/main/java/pl/szymtrener/admin/AdminPostController.java
GET     /admin/statystyki                             AdminStatsController.stats() — src/main/java/pl/szymtrener/admin/AdminStatsController.java
GET     /admin/ustawienia                             AdminSettingsController.form() — src/main/java/pl/szymtrener/admin/AdminSettingsController.java
GET     /admin/zalaczniki/{id}                        AdminAttachmentController.download() — src/main/java/pl/szymtrener/admin/AdminAttachmentController.java
GET     /admin/zalaczniki/{id}/podglad                AdminAttachmentController.preview() — src/main/java/pl/szymtrener/admin/AdminAttachmentController.java
GET     /admin/zgloszenia                             AdminSubmissionController.list() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
GET     /admin/zgloszenia/{id}                        AdminSubmissionController.detail() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
GET     /admin/zgloszenia/{id}/dane                   AdminSubmissionController.export() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
GET     /admin/zgloszenia/{id}/szablon/{code}         AdminSubmissionController.template() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
GET     /admin/zgody-cookies                          AdminConsentController.list() — src/main/java/pl/szymtrener/admin/AdminConsentController.java
GET     /admin/zgody-cookies/{key}/dane               AdminConsentController.export() — src/main/java/pl/szymtrener/admin/AdminConsentController.java
GET     /blog                                         BlogController.list() — src/main/java/pl/szymtrener/web/BlogController.java
GET     /blog/kategoria/{slug}                        BlogController.byCategory() — src/main/java/pl/szymtrener/web/BlogController.java
GET     /blog/szukaj                                  BlogController.search() — src/main/java/pl/szymtrener/web/BlogController.java
GET     /blog/{slug}                                  BlogController.post() — src/main/java/pl/szymtrener/web/BlogController.java
GET     /feed.xml                                     SeoController.feed() — src/main/java/pl/szymtrener/seo/SeoController.java
GET     /llms.txt                                     SeoController.llms() — src/main/java/pl/szymtrener/seo/SeoController.java
GET     /media/**                                     MediaController.serve() — src/main/java/pl/szymtrener/media/MediaController.java
GET     /pliki/{id}/**                                MediaController.download() — src/main/java/pl/szymtrener/media/MediaController.java
GET     /polityka-prywatnosci                         HomeController.privacy() — src/main/java/pl/szymtrener/web/HomeController.java
GET     /regulamin                                    HomeController.terms() — src/main/java/pl/szymtrener/web/HomeController.java
GET     /robots.txt                                   SeoController.robots() — src/main/java/pl/szymtrener/seo/SeoController.java
GET     /sitemap.xml                                  SeoController.sitemap() — src/main/java/pl/szymtrener/seo/SeoController.java
GET     /ustawienia-cookies                           ConsentController.settings() — src/main/java/pl/szymtrener/web/ConsentController.java
GET     /ustawienia-cookies/moje-dane                 ConsentController.myData() — src/main/java/pl/szymtrener/web/ConsentController.java
GET     /{key}.txt                                    SeoController.indexNowKey() — src/main/java/pl/szymtrener/seo/SeoController.java
POST    /admin/administratorzy                        AdminUsersController.add() — src/main/java/pl/szymtrener/admin/AdminUsersController.java
POST    /admin/administratorzy/{id}                   AdminUsersController.update() — src/main/java/pl/szymtrener/admin/AdminUsersController.java
POST    /admin/administratorzy/{id}/usun              AdminUsersController.delete() — src/main/java/pl/szymtrener/admin/AdminUsersController.java
POST    /admin/api/import-docx                        AdminApiController.importDocument() — src/main/java/pl/szymtrener/admin/AdminApiController.java
POST    /admin/api/media                              AdminApiController.upload() — src/main/java/pl/szymtrener/admin/AdminApiController.java
POST    /admin/haslo                                  AdminController.changePassword() — src/main/java/pl/szymtrener/admin/AdminController.java
POST    /admin/klienci/zapisz                         AdminTraineeController.save() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/ze-zgloszenia/{submissionId}   AdminTraineeController.fromSubmission() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/{id}/notatka                   AdminTraineeController.addNote() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/{id}/pakiet                    AdminTraineeController.sellPackage() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/{id}/pomiar                    AdminTraineeController.addMeasurement() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/{id}/sesja                     AdminTraineeController.saveSession() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/{id}/sesja/{sessionId}/usun    AdminTraineeController.deleteSession() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/{id}/usun                      AdminTraineeController.delete() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/klienci/{id}/wiadomosc                 AdminTraineeController.message() — src/main/java/pl/szymtrener/admin/AdminTraineeController.java
POST    /admin/media/{id}/usun                        AdminMediaController.delete() — src/main/java/pl/szymtrener/admin/AdminMediaController.java
POST    /admin/notatki/{id}/przypnij                  NoteController.togglePin() — src/main/java/pl/szymtrener/admin/NoteController.java
POST    /admin/notatki/{id}/usun                      NoteController.delete() — src/main/java/pl/szymtrener/admin/NoteController.java
POST    /admin/oferta/dieta                           AdminOfferController.saveDiet() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/faq                             AdminOfferController.addQuestion() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/faq/{id}                        AdminOfferController.saveQuestion() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/faq/{id}/usun                   AdminOfferController.deleteQuestion() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/jednorazowe                     AdminOfferController.saveOneOff() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/konsultacja                     AdminOfferController.saveConsultation() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/opinie                          AdminOfferController.addTestimonial() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/opinie/{id}                     AdminOfferController.saveTestimonial() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/opinie/{id}/usun                AdminOfferController.deleteTestimonial() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/pakiety                         AdminOfferController.addPackage() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/pakiety/{id}                    AdminOfferController.savePackage() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/pakiety/{id}/usun               AdminOfferController.deletePackage() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/stacjonarnie                    AdminOfferController.addStationary() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/stacjonarnie/zasady             AdminOfferController.saveRules() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/stacjonarnie/{id}               AdminOfferController.saveStationary() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/oferta/stacjonarnie/{id}/usun          AdminOfferController.deleteStationary() — src/main/java/pl/szymtrener/admin/AdminOfferController.java
POST    /admin/posty/autozapis                        AdminPostController.autosave() — src/main/java/pl/szymtrener/admin/AdminPostController.java
POST    /admin/posty/ocena                            AdminPostController.score() — src/main/java/pl/szymtrener/admin/AdminPostController.java
POST    /admin/posty/zapisz                           AdminPostController.save() — src/main/java/pl/szymtrener/admin/AdminPostController.java
POST    /admin/posty/{id}/usun                        AdminPostController.delete() — src/main/java/pl/szymtrener/admin/AdminPostController.java
POST    /admin/ustawienia                             AdminSettingsController.save() — src/main/java/pl/szymtrener/admin/AdminSettingsController.java
POST    /admin/ustawienia/szablony/{id}               AdminSettingsController.saveTemplate() — src/main/java/pl/szymtrener/admin/AdminSettingsController.java
POST    /admin/ustawienia/test-poczty                 AdminSettingsController.testMail() — src/main/java/pl/szymtrener/admin/AdminSettingsController.java
POST    /admin/zgloszenia/{id}/etap                   AdminSubmissionController.stage() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/konwertuj              AdminSubmissionController.convert() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/notatka                AdminSubmissionController.addNote() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/notatka/tagi           AdminSubmissionController.addTaggedNote() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/przypomnienie          AdminSubmissionController.remind() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/przypomnienie/zalatwione AdminSubmissionController.remindDone() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/status                 AdminSubmissionController.changeStatus() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/usun                   AdminSubmissionController.delete() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgloszenia/{id}/wiadomosc              AdminSubmissionController.message() — src/main/java/pl/szymtrener/admin/AdminSubmissionController.java
POST    /admin/zgody-cookies/{key}/usun               AdminConsentController.delete() — src/main/java/pl/szymtrener/admin/AdminConsentController.java
POST    /api/zgloszenia/kontakt                       PublicFormController.contact() — src/main/java/pl/szymtrener/web/PublicFormController.java
POST    /api/zgloszenia/online                        PublicFormController.online() — src/main/java/pl/szymtrener/web/PublicFormController.java
POST    /zgoda-cookies                                ConsentController.consent() — src/main/java/pl/szymtrener/web/ConsentController.java

## Metody (per klasa)

### AdminAccountInitializer  (src/main/java/pl/szymtrener/admin/AdminAccountInitializer.java)
- [   ] syncAdminAccount(AdminUserRepository repo, PasswordEncoder encoder, AppProperties props, SettingsService settings, ConfigurableEnvironment environment): ApplicationRunner  :43
- [prv] resolveAccount(AdminUserRepository repo, String email): AdminUser  :115
- [prv] sourceOf(ConfigurableEnvironment environment, String name): String  :127
- [prv] addresses(AdminUserRepository repo): String  :135
- [prv] fingerprint(String email, String password): String  :142
- [prv] trim(String value): String  :152

### AdminAccountSyncIT  (src/test/java/pl/szymtrener/admin/AdminAccountSyncIT.java)
- [prv] sync(String email, String password): void  :30
- [   ] clean(): void  :41
- [   ] createsAccountOnFirstRun(): void  :48
- [   ] updatesPassword(): void  :58
- [   ] renamesInsteadOfDuplicating(): void  :70
- [   ] panelPasswordSurvivesRestart(): void  :81

### AdminApiController  (src/main/java/pl/szymtrener/admin/AdminApiController.java)
- [prv] describe(MediaFile file): Map<String, Object>  :62

### AdminAttachmentController  (src/main/java/pl/szymtrener/admin/AdminAttachmentController.java)
- [pub] download(@PathVariable Long id): ResponseEntity<byte[]>  :38
- [prv] find(Long id): AttachmentService.Loaded  :60
- [prv] respond(AttachmentService.Loaded file, ContentDisposition.Builder disposition, CacheControl cache, String etag): ResponseEntity<byte[]>  :64

### AdminAttachmentControllerTest  (src/test/java/pl/szymtrener/admin/AdminAttachmentControllerTest.java)
- [prv] file(long id, String name, String mime, boolean previewable): void  :24
- [   ] downloadHeaders(): void  :36
- [   ] previewOnlyImages(): void  :50

### AdminConsentController  (src/main/java/pl/szymtrener/admin/AdminConsentController.java)
- [prv] notFound(String key): NotFoundException  :90

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
- [pub] publishedPosts(): long  :38
- [pub] newSubmissions(): long  :43
- [pub] unreadSubmissionReplies(): long  :48
- [pub] unreadClientReplies(): long  :53
- [pub] dueReminders(): long  :62
- [pub] currentName(): String  :72
- [pub] currentInitials(): String  :81
- [prv] currentLogin(): String  :90

### AdminOfferController  (src/main/java/pl/szymtrener/admin/AdminOfferController.java)
- [pub] overview(Model model): String  :60
- [pub] saveOneOff(@RequestParam String combo, @RequestParam String update, RedirectAttributes flash): String  :114
- [pub] addPackage(@RequestParam String name, RedirectAttributes flash): String  :130
- [pub] editPackage(@PathVariable Long id, Model model): String  :148
- [pub] deletePackage(@PathVariable Long id, RedirectAttributes flash): String  :221
- [pub] addTestimonial(@RequestParam String name, @RequestParam String body, RedirectAttributes flash): String  :232
- [pub] editTestimonial(@PathVariable Long id, Model model): String  :248
- [pub] deleteTestimonial(@PathVariable Long id, RedirectAttributes flash): String  :286
- [pub] addQuestion(@RequestParam String question, RedirectAttributes flash): String  :297
- [pub] deleteQuestion(@PathVariable Long id, RedirectAttributes flash): String  :339
- [pub] stationary(Model model): String  :350
- [prv] pricesInZloty(List<StationaryPackage> all): Map<Long, String>  :363
- [pub] addStationary(@RequestParam StationaryKind kind, @RequestParam String name, RedirectAttributes flash): String  :432
- [pub] deleteStationary(@PathVariable Long id, RedirectAttributes flash): String  :453
- [prv] text(String v): String  :477
- [   ] optionalInt(String input): Integer  :482
- [   ] grosze(String input): Integer  :497
- [   ] zlote(int grosze): String  :512
- [prv] blankToNull(String v): String  :517

### AdminOfferControllerTest  (src/test/java/pl/szymtrener/admin/AdminOfferControllerTest.java)
- [   ] parsesHumanInput(): void  :17
- [   ] rejectsNonAmounts(): void  :28
- [   ] formatsBackForForm(): void  :39
- [   ] roundTrips(): void  :48

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
- [pub] form(Model model): String  :49
- [pub] testMail(RedirectAttributes flash): String  :76

### AdminSubmissionController  (src/main/java/pl/szymtrener/admin/AdminSubmissionController.java)
- [pub] detail(@PathVariable Long id, Model model): String  :82
- [pub] addNote(@PathVariable Long id, @RequestParam String body, Principal principal): String  :111
- [pub] stage(@PathVariable Long id, @RequestParam SubmissionStatus status, RedirectAttributes flash): String  :121
- [pub] template(@PathVariable Long id, @PathVariable String code): Map<String, String>  :175
- [pub] remindDone(@PathVariable Long id, RedirectAttributes flash): String  :214
- [pub] convert(@PathVariable Long id, RedirectAttributes flash): String  :222
- [pub] export(@PathVariable Long id): ResponseEntity<byte[]>  :254
- [pub] delete(@PathVariable Long id, Principal principal): String  :270

### AdminTraineeController  (src/main/java/pl/szymtrener/admin/AdminTraineeController.java)
- [pub] week(Model model): String  :111
- [pub] create(Model model): String  :123
- [pub] profile(@PathVariable Long id, Model model): String  :131
- [pub] edit(@PathVariable Long id, Model model): String  :159
- [pub] delete(@PathVariable Long id, RedirectAttributes flash): String  :175
- [pub] deleteSession(@PathVariable Long id, @PathVariable Long sessionId, RedirectAttributes flash): String  :225
- [pub] sellPackage(@PathVariable Long id, @RequestParam String name, @RequestParam int totalSessions, @RequestParam String pricePerSession, RedirectAttributes flash): String  :274
- [pub] fromSubmission(@PathVariable Long submissionId, RedirectAttributes flash): String  :351
- [prv] form(Model model, Trainee trainee): String  :357
- [prv] toForm(Trainee trainee): TraineeForm  :366

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
- [pub] loadUserByUsername(String username): UserDetails  :43
- [prv] matchesEnvironment(String login): boolean  :64
- [prv] trim(String value): String  :73

### AdminUserDetailsServiceTest  (src/test/java/pl/szymtrener/admin/AdminUserDetailsServiceTest.java)
- [prv] service(String envEmail, String envPassword, AdminUser... inDatabase): AdminUserDetailsService  :31
- [prv] account(String email, String password): AdminUser  :49
- [   ] environmentWinsOverDatabase(): void  :59
- [   ] environmentWorksWithEmptyDatabase(): void  :71
- [   ] loginIsTrimmedAndCaseInsensitive(): void  :82
- [   ] fallsBackToDatabaseWhenEnvIncomplete(): void  :90
- [   ] unknownAccountIsRejected(): void  :101

### AdminUserRepository  (src/main/java/pl/szymtrener/admin/AdminUserRepository.java)
- [   ] findByEmailIgnoreCase(String email): Optional<AdminUser>  :7

### AdminUsersController  (src/main/java/pl/szymtrener/admin/AdminUsersController.java)
- [pub] list(Model model, Authentication auth): String  :49
- [pub] edit(@PathVariable Long id, Model model, Authentication auth): String  :88
- [pub] delete(@PathVariable Long id, Authentication auth, RedirectAttributes flash): String  :158
- [prv] fill(Model model, Authentication auth): void  :178

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
- [pro] shouldNotFilter(HttpServletRequest request): boolean  :55
- [prv] record(HttpServletRequest request): void  :77
- [prv] clientIp(HttpServletRequest request): String  :97
- [prv] referrerHost(String referrer): String  :104
- [prv] hash(String input): String  :113

### AnalyticsView  (src/main/java/pl/szymtrener/analytics/AnalyticsView.java)
- [pub] bars(Instant since, int range): List<Bar>  :52
- [pub] rows(List<Object[]> source, long total, int limit): List<Row>  :76
- [pub] trend(long current, long previous): Trend  :92
- [pub] trendOverDays(int days, java.util.function.BiFunction<Instant, Instant, Long> counter): Trend  :101

### AppProperties  (src/main/java/pl/szymtrener/config/AppProperties.java)
- [pub] absolute(String path): String  :23
- [pub] siteHost(): String  :29

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

### AttachmentFile  (src/main/java/pl/szymtrener/crm/AttachmentFile.java)
- [pub] toString(): String  :10

### AttachmentIT  (src/test/java/pl/szymtrener/crm/AttachmentIT.java)
- [   ] replyFormWithFiles(): void  :41
- [   ] attachmentLifecycle(): void  :69

### AttachmentLimits  (src/main/java/pl/szymtrener/crm/AttachmentLimits.java)
- [pub] maxFiles(): int  :13
- [pub] maxTotalBytes(): long  :15
- [pub] maxFileBytes(): long  :17

### AttachmentPolicy  (src/main/java/pl/szymtrener/crm/AttachmentPolicy.java)
- [pub] ok(): boolean  :59
- [pub] forReply(String way, List<MultipartFile> uploads): Outgoing  :63
- [pub] outgoing(List<MultipartFile> uploads): Outgoing  :72
- [   ] rejectBeforeReading(String rawName, int acceptedSoFar, List<String> notes): boolean  :107
- [   ] decoration(boolean inlineWithContentId, boolean image, int size): boolean  :121
- [   ] incoming(String rawName, byte[] data, boolean truncated, int acceptedSoFar, long bytesSoFar, List<String> notes): AttachmentFile  :131
- [   ] detect(byte[] d, String name): String  :151
- [   ] dangerous(byte[] d, String name): boolean  :178
- [   ] safeName(String raw): String  :203
- [prv] trimDotsAndSpaces(String name): String  :222
- [prv] extension(String name): String  :226
- [prv] plainText(byte[] d): boolean  :232
- [prv] startsWith(byte[] d, String prefix): boolean  :239
- [prv] startsWith(byte[] d, int... prefix): boolean  :243
- [prv] ascii(byte[] d, int from, int length): String  :251

### AttachmentPolicyTest  (src/test/java/pl/szymtrener/crm/AttachmentPolicyTest.java)
- [prv] upload(String name, byte[] data): MultipartFile  :24
- [   ] detectsByContent(): void  :30
- [   ] outgoingAccepts(): void  :41
- [   ] outgoingRejects(): void  :50
- [   ] incoming(): void  :62
- [   ] safeName(): void  :81

### AttachmentService  (src/main/java/pl/szymtrener/crm/AttachmentService.java)
- [pub] store(Long messageId, List<AttachmentFile> files): void  :34
- [pub] forMessages(Collection<Long> messageIds): Map<Long, List<MessageAttachment>>  :44
- [pub] describe(Long id): Optional<MessageAttachment>  :52
- [pub] load(Long id): Optional<Loaded>  :57

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

### Bytes  (src/main/java/pl/szymtrener/common/Bytes.java)
- [pub] human(long bytes): String  :16
- [pub] sha256(byte[] data): String  :22

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
- [pub] purgeOldPageViews(): void  :38
- [pub] purgeOldConsents(): void  :47

### ClientInsightService  (src/main/java/pl/szymtrener/crm/ClientInsightService.java)
- [pub] signedDelta(): String  :57
- [pub] packageState(Long traineeId): PackageState  :73
- [prv] usedSessions(Long traineeId, Long packageId): int  :100
- [pub] endingSoon(): List<Trainee>  :109
- [pub] staleContacts(int days): List<Trainee>  :118
- [pub] nextSession(Long traineeId): Optional<TrainingSession>  :128
- [pub] log(Long traineeId, int limit): List<TrainingSession>  :136
- [pub] thisWeek(): List<TrainingSession>  :144
- [pub] attendancePct(Long traineeId): int  :156
- [pub] doneSessions(Long traineeId): long  :165
- [pub] cancelledSessions(Long traineeId): long  :171
- [pub] lifetimeValueGr(Long traineeId): int  :180
- [pub] lifetimeValue(Long traineeId): String  :186
- [pub] soldThisMonth(): String  :192
- [pub] progress(Long traineeId): List<MeasurementRow>  :207
- [pub] weightChange(Long traineeId): MeasurementRow  :235
- [prv] trim(BigDecimal value): String  :242
- [pub] rows(List<Trainee> source, int staleDays): List<ClientRow>  :250

### ConsentController  (src/main/java/pl/szymtrener/web/ConsentController.java)
- [pub] settings(Model model): String  :64
- [pub] myData(HttpServletRequest request): ResponseEntity<byte[]>  :126
- [   ] safeRedirect(String path): String  :133
- [prv] unsafeInRedirect(int c): boolean  :146

### ConsentControllerIT  (src/test/java/pl/szymtrener/web/ConsentControllerIT.java)
- [   ] clean(): void  :49
- [   ] pagesRenderWithoutDecision(): void  :56
- [   ] rejectsWithoutCsrf(): void  :68
- [   ] acceptStoresConsentAndCookie(): void  :75
- [   ] changeKeepsKey(): void  :94
- [   ] safeRedirect(): void  :115
- [   ] myDataOnlyForOwnCookie(): void  :126
- [   ] analyticsLinksConsent(): void  :147
- [   ] countsReturningAcrossDays(): void  :164
- [   ] secureCookieUsesHostPrefix(): void  :179
- [   ] viewIsBufferedBeforeCommit(@Autowired org.thymeleaf.spring6.view.ThymeleafViewResolver resolver): void  :194
- [   ] limitsNewConsentsPerClient(): void  :200
- [   ] adminRequiresLogin(): void  :215
- [   ] adminSearchExportDelete(): void  :229
- [   ] statsRender(): void  :255
- [prv] fetchConsent(): MockHttpServletRequestBuilder  :261
- [prv] cookie(UUID key): Cookie  :265

### ConsentCookie  (src/main/java/pl/szymtrener/consent/ConsentCookie.java)
- [   ] read(HttpServletRequest request): Optional<UUID>  :32
- [pub] write(UUID key, HttpServletRequest request, HttpServletResponse response): void  :46
- [prv] name(HttpServletRequest request): String  :57

### ConsentService  (src/main/java/pl/szymtrener/consent/ConsentService.java)
- [pub] current(HttpServletRequest request): Optional<Status>  :58
- [pub] record(UUID existingKey, boolean statistics, ConsentSource source): UUID  :75
- [pub] export(UUID key): Optional<Map<String, Object>>  :92
- [pub] delete(UUID key): boolean  :121
- [pub] search(String query, int page): Page<Row>  :131
- [pub] purgeNotChangedSince(Instant cutoff): int  :145
- [pub] parseKey(String raw): Optional<UUID>  :150

### ConsentServiceIT  (src/test/java/pl/szymtrener/consent/ConsentServiceIT.java)
- [   ] clean(): void  :26
- [   ] recordsHistory(): void  :33
- [   ] unknownKeyStartsNewConsent(): void  :46
- [   ] exportsOwnData(): void  :58
- [   ] deletesEverythingLinked(): void  :76
- [   ] purgesStaleConsents(): void  :94
- [   ] searchesByPrefix(): void  :111
- [prv] idOf(UUID key): Long  :123
- [prv] pageView(String path, Long consentId): void  :127

### ConsentSource  (src/main/java/pl/szymtrener/consent/ConsentSource.java)
- [pub] parse(String raw): ConsentSource  :7

### ConsentView  (src/main/java/pl/szymtrener/consent/ConsentView.java)
- [pub] current(): ConsentService.Status  :23
- [pub] returnPath(): String  :32
- [prv] request(): HttpServletRequest  :37

### ConsentedPageViews  (src/main/java/pl/szymtrener/analytics/ConsentedPageViews.java)
- [pub] find(long consentId): List<View>  :19

### ConsentedViews  (src/main/java/pl/szymtrener/consent/ConsentedViews.java)
- [   ] find(long consentId): List<View>  :15

### Content  (src/main/java/pl/szymtrener/crm/InboundMail.java)
- [   ] parse(MimeMessage message, Instant now): InboundMail  :45
- [prv] replyTo(MimeMessage message): List<String>  :65
- [prv] ids(String header): List<String>  :77
- [prv] firstId(String header): String  :85
- [prv] sender(MimeMessage message): String  :90
- [prv] automatic(MimeMessage message): boolean  :102
- [prv] collect(Part part, Content content, int depth): void  :111
- [prv] text(Part part): String  :138
- [prv] read(Part part): Object  :153
- [prv] attachment(Part part, Content content): void  :162
- [prv] rethrowConnectionError(IOException e): void  :192
- [prv] fileName(Part part): String  :196
- [pub] toString(): String  :215

### ContentMetrics  (src/main/java/pl/szymtrener/content/ContentMetrics.java)
- [pub] analyse(String contentHtml, String lead): Result  :15

### CookieConsent  (src/main/java/pl/szymtrener/consent/CookieConsent.java)
- [   ] change(boolean statistics): void  :28
- [pub] getId(): Long  :33
- [pub] getConsentKey(): UUID  :34
- [pub] isStatistics(): boolean  :35
- [pub] getCreatedAt(): Instant  :36
- [pub] getUpdatedAt(): Instant  :37

### CookieConsentChange  (src/main/java/pl/szymtrener/consent/CookieConsentChange.java)
- [pub] isStatistics(): boolean  :31
- [pub] getSource(): ConsentSource  :32
- [pub] getPolicyVersion(): String  :33
- [pub] getChangedAt(): Instant  :34

### CookieConsentChangeRepository  (src/main/java/pl/szymtrener/consent/CookieConsentChangeRepository.java)
- [   ] findByConsentIdOrderByChangedAtDesc(Long consentId): List<CookieConsentChange>  :9

### CookieConsentRepository  (src/main/java/pl/szymtrener/consent/CookieConsentRepository.java)
- [   ] findByConsentKey(UUID consentKey): Optional<CookieConsent>  :16

### CrmModelTest  (src/test/java/pl/szymtrener/crm/CrmModelTest.java)
- [   ] cancelledDoesNotConsume(): void  :22
- [   ] packageValue(): void  :35
- [   ] warningNotes(): void  :46
- [   ] systemAndFailedMessages(): void  :62
- [   ] contactLabels(): void  :79

### DatabaseUrlEnvironmentPostProcessor  (src/main/java/pl/szymtrener/config/DatabaseUrlEnvironmentPostProcessor.java)
- [pub] postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application): void  :40
- [   ] translate(String raw): Map<String, Object>  :68
- [prv] stripScheme(String raw): String  :138
- [prv] parsePort(String value): int  :145
- [prv] decode(String value): String  :161

### DatabaseUrlEnvironmentPostProcessorTest  (src/test/java/pl/szymtrener/config/DatabaseUrlEnvironmentPostProcessorTest.java)
- [   ] translatesPlainUrl(): void  :23
- [   ] decodesEncodedPassword(): void  :34
- [   ] defaultsPort(): void  :43
- [   ] keepsQueryString(): void  :52
- [   ] acceptsBothSchemes(): void  :62
- [   ] leavesJdbcUrlAlone(): void  :69
- [   ] nonPostgresValueIsIgnored(): void  :76
- [   ] translatesRealCoolifyUrl(): void  :82
- [   ] handlesUnencodedPassword(String password): void  :95
- [   ] acceptsUnderscoreInHost(): void  :106
- [   ] brokenPostgresUrlFailsLoudly(String raw): void  :135
- [   ] brokenDatabaseUrlDoesNotFallBackToDbUrl(): void  :147
- [   ] percentFollowedByHexIsDecoded(): void  :160
- [   ] acceptsMultipleHosts(): void  :169
- [   ] schemeIsCaseInsensitive(): void  :176
- [   ] dropsFragment(): void  :183
- [   ] encodedAtInQueryKeepsHost(): void  :190
- [   ] postProcessorOverridesDatasource(): void  :198

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

### Downloads  (src/main/java/pl/szymtrener/common/Downloads.java)
- [pub] json(byte[] body, String filename): ResponseEntity<byte[]>  :15

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

### FakeMailbox  (src/test/java/pl/szymtrener/crm/FakeMailbox.java)
- [   ] mail(long uid, String from): MimeMessage  :23
- [pro] updateMessageID(): void  :26

### GanwilkArchitectureTest  (src/test/java/pl/szymtrener/arch/GanwilkArchitectureTest.java)
- [   ] importClasses(): void  :35
- [   ] brak_cykli_miedzy_pakietami(): void  :43
- [   ] kontrolery_nie_siegaja_wprost_do_repozytoriow(): void  :53
- [   ] transakcje_nie_na_kontrolerach(): void  :63
- [   ] serwisy_nie_zaleza_od_warstwy_web(): void  :74

### GlobalExceptionHandler  (src/main/java/pl/szymtrener/common/GlobalExceptionHandler.java)
- [pub] tooLarge(MaxUploadSizeExceededException exception, HttpServletRequest request, HttpServletResponse response): Object  :50
- [pub] handle(Exception exception, HttpServletRequest request): Object  :72
- [prv] wantsJson(HttpServletRequest request): boolean  :96

### GlobalExceptionHandlerTest  (src/test/java/pl/szymtrener/common/GlobalExceptionHandlerTest.java)
- [   ] rethrowsMissingStaticResource(): void  :25
- [   ] rethrowsAnnotatedException(): void  :34
- [   ] handlesRealFailure(): void  :43

### HomeController  (src/main/java/pl/szymtrener/web/HomeController.java)
- [pub] home(Model model): String  :43
- [pub] privacy(Model model): String  :80
- [pub] terms(Model model): String  :92

### HomeFaq  (src/main/java/pl/szymtrener/web/HomeFaq.java)
- [pub] of(StationaryOfferService.Page stationary, String lowestMonthly, SimplePriceView consult, SimplePriceView diet): HomeFaq  :21
- [pub] json(): HomeFaq  :56
- [   ] quote(String value): String  :64

### HomePageCopyIT  (src/test/java/pl/szymtrener/web/HomePageCopyIT.java)
- [prv] home(): String  :46
- [   ] newPricesAreRendered(): void  :55
- [   ] faqAndStructuredData(): void  :81
- [   ] generalCopyRules(): void  :116
- [   ] wojtekTestimonial(): void  :133
- [   ] invalidTermIsRejected(): void  :144
- [   ] structuredDataSurvivesHostileName(): void  :165

### HtmlSanitizer  (src/main/java/pl/szymtrener/content/HtmlSanitizer.java)
- [pub] clean(String html): String  :52

### HtmlSanitizerTest  (src/test/java/pl/szymtrener/HtmlSanitizerTest.java)
- [   ] usuwaSkryptyIStyleZWklejonejTresci(): void  :13
- [   ] zachowujeTabeleZNaglowkami(): void  :22
- [   ] zachowujeBlokFilmuZEdytora(): void  :31
- [   ] dodajeLazyLoadingObrazkom(): void  :39

### HttpThumbnailFetcher  (src/main/java/pl/szymtrener/media/HttpThumbnailFetcher.java)
- [pub] fetch(String videoId): Optional<byte[]>  :33
- [   ] jpeg(byte[] bytes): Optional<byte[]>  :56

### ImapMailbox  (src/main/java/pl/szymtrener/crm/ImapMailbox.java)
- [pub] open(String host, int port, String user, String password): ImapMailbox  :36
- [pub] uidValidity(): long  :61
- [pub] highestUid(): long  :66
- [pub] fetchAfter(long uid, int limit): List<Fetched>  :72
- [pub] close(): void  :86
- [prv] closeQuietly(Store store): void  :95

### InboundMailIT  (src/test/java/pl/szymtrener/crm/InboundMailIT.java)
- [   ] repliesLandInThread(): void  :33
- [   ] uniqueMessageId(): void  :81
- [prv] inboundMessage(Long submissionId, String id): Message  :95

### InboundMailScheduler  (src/main/java/pl/szymtrener/scheduler/InboundMailScheduler.java)
- [pub] poll(): void  :45
- [prv] status(String text): void  :63

### InboundMailService  (src/main/java/pl/szymtrener/crm/InboundMailService.java)
- [pub] poll(Mailbox mailbox, String ownAddress): Result  :52
- [prv] attempt(long uid): int  :98
- [prv] record(Mailbox.Fetched fetched, long validity, String ownAddress): boolean  :106

### InboundMailServiceTest  (src/test/java/pl/szymtrener/crm/InboundMailServiceTest.java)
- [   ] setUp(): void  :41
- [   ] firstRunStartsAtEnd(): void  :60
- [   ] recordsNewAndAdvances(): void  :73
- [   ] skipsWhatIsNotAReply(): void  :89
- [   ] brokenMessageDoesNotBlock(): void  :111
- [   ] closedFolderKeepsProgress(): void  :127
- [   ] transientErrorRetriedThenSkipped(): void  :141
- [   ] qpBombDoesNotBlockInbox(): void  :161
- [   ] uidValidityChange(): void  :193
- [prv] mail(long uid, String from): MimeMessage  :203

### InboundMailTest  (src/test/java/pl/szymtrener/crm/InboundMailTest.java)
- [prv] message(): MimeMessage  :26
- [   ] gmailPolishQuote(): void  :38
- [   ] otherClients(): void  :55
- [   ] plainOdIsNotAQuote(): void  :66
- [   ] inlineAndOnlyQuote(): void  :73
- [   ] limitsLength(): void  :80
- [   ] htmlToText(): void  :88
- [   ] plainReply(): void  :107
- [   ] multipartWithAttachment(): void  :128
- [   ] rejectsDangerousAndDecorations(): void  :154
- [   ] connectionDropWhileReadingAttachment(): void  :182
- [pub] getInputStream(): java.io.InputStream  :185
- [pub] read(): int  :188
- [   ] attachmentOnly(): void  :209
- [   ] htmlOnly(): void  :226
- [   ] automaticAndFutureDate(): void  :236
- [   ] toStringHidesContent(): void  :260

### InboundMailText  (src/main/java/pl/szymtrener/crm/InboundMailText.java)
- [   ] clean(String raw): String  :48
- [   ] htmlToText(String html): String  :59
- [pub] head(Node node, int depth): void  :65
- [pub] tail(Node node, int depth): void  :75
- [prv] newline(StringBuilder out): void  :82
- [prv] withoutQuote(String text): String  :86
- [prv] quoteStart(List<String> lines): int  :94
- [prv] normalize(String text): String  :113

### InboundMatcher  (src/main/java/pl/szymtrener/crm/InboundMatcher.java)
- [pub] match(InboundMail mail): Optional<Target>  :30
- [prv] byReply(InboundMail mail): Optional<Target>  :34
- [prv] byTrainee(InboundMail mail): Optional<Target>  :40
- [prv] bySubmission(InboundMail mail): Optional<Target>  :46
- [prv] traineeOf(Long traineeId, Long submissionId): Long  :56

### InboundMatcherTest  (src/test/java/pl/szymtrener/crm/InboundMatcherTest.java)
- [prv] mail(List<String> replyTo, String from): InboundMail  :23
- [   ] replyToSubmissionMessageAfterConversion(): void  :29
- [   ] addressFallback(): void  :43

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

### LegalDocumentsIT  (src/test/java/pl/szymtrener/web/LegalDocumentsIT.java)
- [prv] page(String path): String  :33
- [   ] termsPage(): void  :42
- [   ] privacyPage(): void  :59
- [   ] termsAreLinked(): void  :72
- [   ] pdfsAreServed(): void  :83
- [   ] formsInformInsteadOfAskingForConsent(): void  :98
- [   ] rulesFollowStudioTerms(): void  :109
- [   ] videoThumbnailsAreSelfHosted(): void  :125
- [   ] fontsAreSelfHosted(): void  :139

### LoginAuditListener  (src/main/java/pl/szymtrener/admin/LoginAuditListener.java)
- [pub] onSuccess(AuthenticationSuccessEvent event): void  :34
- [pub] onFailure(AbstractAuthenticationFailureEvent event): void  :39
- [prv] existingAddresses(): String  :54

### MailConfig  (src/main/java/pl/szymtrener/config/MailConfig.java)
- [   ] reportConfiguration(): void  :46
- [pub] mask(String address): String  :81

### MailService  (src/main/java/pl/szymtrener/submission/MailService.java)
- [pub] sendNotifications(Submission s): void  :44
- [prv] enabled(): boolean  :73
- [prv] trainerNotification(Submission s): MimeMessage  :84
- [prv] autoReply(Submission s): MimeMessage  :102
- [   ] plainAutoReply(Submission s): String  :117
- [prv] plainTrainerNotification(Submission s): String  :132
- [prv] line(StringBuilder sb, String label, String value): void  :148

### MailTemplatesTest  (src/test/java/pl/szymtrener/submission/MailTemplatesTest.java)
- [   ] setUp(): void  :27
- [prv] submission(): Submission  :39
- [prv] context(Submission s): Context  :54
- [   ] trainerNotificationRenders(): void  :65
- [   ] trainerNotificationDoesNotClaimConsent(): void  :79
- [   ] consentLabelOnlyForLegacySubmissions(): void  :87
- [   ] skipsEmptyFields(): void  :99
- [   ] showsOfferContext(): void  :115
- [   ] skipsOfferContextWhenAbsent(): void  :129
- [   ] clientConfirmationRenders(): void  :137
- [   ] firstNameIsCapitalized(): void  :150
- [   ] plainConfirmationGreetsByFirstName(): void  :160
- [   ] greetingWithoutName(): void  :178
- [   ] bothTemplatesSurviveMinimalSubmission(): void  :188

### Mailbox  (src/main/java/pl/szymtrener/crm/Mailbox.java)
- [   ] uidValidity(): long  :16
- [   ] highestUid(): long  :19
- [   ] fetchAfter(long uid, int limit): List<Fetched>  :22
- [   ] close(): void  :27

### Measurement  (src/main/java/pl/szymtrener/crm/Measurement.java)
- [pub] getId(): Long  :27
- [pub] getTraineeId(): Long  :28
- [pub] setTraineeId(Long traineeId): void  :29
- [pub] getTakenOn(): LocalDate  :30
- [pub] setTakenOn(LocalDate takenOn): void  :31
- [pub] getMetric(): String  :32
- [pub] setMetric(String metric): void  :33
- [pub] getValue(): BigDecimal  :34
- [pub] setValue(BigDecimal value): void  :35
- [pub] getUnit(): String  :36
- [pub] setUnit(String unit): void  :37
- [pub] isLowerIsBetter(): boolean  :38
- [pub] setLowerIsBetter(boolean lowerIsBetter): void  :39

### MeasurementRepository  (src/main/java/pl/szymtrener/crm/MeasurementRepository.java)
- [   ] findByTraineeIdOrderByMetricAscTakenOnAsc(Long traineeId): List<Measurement>  :8

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
- [   ] findFirstByOriginalName(String originalName): Optional<MediaFile>  :12
- [   ] findAllByOrderByCreatedAtDesc(Pageable pageable): Page<MediaFile>  :13
- [   ] findByKindOrderByCreatedAtDesc(MediaKind kind, Pageable pageable): Page<MediaFile>  :14
- [   ] countByKind(MediaKind kind): long  :16
- [   ] totalBytes(): long  :20

### MediaService  (src/main/java/pl/szymtrener/media/MediaService.java)
- [pub] upload(MultipartFile file, String altText): MediaFile  :38
- [pub] store(byte[] bytes, String originalName, String mime, String altText): MediaFile  :97
- [pub] bytes(Long mediaId): Optional<byte[]>  :131
- [pub] byStorageKey(String key): Optional<MediaFile>  :136
- [pub] byId(Long id): Optional<MediaFile>  :141
- [pub] countDownload(Long id): void  :146
- [pub] delete(Long id): void  :155
- [pub] publicUrl(Long mediaId): String  :162
- [prv] extensionFor(String mime): String  :168
- [prv] safeName(String name): String  :178

### Message  (src/main/java/pl/szymtrener/crm/Message.java)
- [pub] system(): boolean  :51
- [pub] outgoing(): boolean  :56
- [pub] failed(): boolean  :62
- [pub] matchedByAddress(): boolean  :68
- [pub] sentLabel(): String  :73
- [pub] getId(): Long  :78
- [pub] getSubmissionId(): Long  :79
- [pub] setSubmissionId(Long submissionId): void  :80
- [pub] getTraineeId(): Long  :81
- [pub] setTraineeId(Long traineeId): void  :82
- [pub] getDirection(): MessageDirection  :83
- [pub] setDirection(MessageDirection direction): void  :84
- [pub] getChannel(): MessageChannel  :85
- [pub] setChannel(MessageChannel channel): void  :86
- [pub] getBody(): String  :87
- [pub] setBody(String body): void  :88
- [pub] getAttachmentId(): Long  :89
- [pub] setAttachmentId(Long attachmentId): void  :90
- [pub] getSentAt(): Instant  :91
- [pub] setSentAt(Instant sentAt): void  :92
- [pub] getMailStatus(): String  :93
- [pub] setMailStatus(String mailStatus): void  :94
- [pub] getMailMessageId(): String  :95
- [pub] setMailMessageId(String mailMessageId): void  :96
- [pub] isUnread(): boolean  :97
- [pub] setUnread(boolean unread): void  :98
- [pub] getMatchedBy(): MatchedBy  :99
- [pub] setMatchedBy(MatchedBy matchedBy): void  :100

### MessageAttachment  (src/main/java/pl/szymtrener/crm/MessageAttachment.java)
- [pub] previewable(): boolean  :34
- [pub] sizeLabel(): String  :39
- [pub] getId(): Long  :43
- [pub] getMessageId(): Long  :44
- [pub] getOriginalName(): String  :45
- [pub] getMimeType(): String  :46
- [pub] getSha256(): String  :47

### MessageAttachmentBlob  (src/main/java/pl/szymtrener/crm/MessageAttachmentBlob.java)
- [pub] getData(): byte[]  :29
- [pub] getId(): Long  :32
- [pub] isNew(): boolean  :36

### MessageAttachmentRepository  (src/main/java/pl/szymtrener/crm/MessageAttachmentRepository.java)
- [   ] findByMessageIdInOrderByIdAsc(Collection<Long> messageIds): List<MessageAttachment>  :11

### MessageChannel  (src/main/java/pl/szymtrener/crm/MessageChannel.java)
- [pub] label(): String  :16
- [pub] css(): String  :19

### MessageRepository  (src/main/java/pl/szymtrener/crm/MessageRepository.java)
- [   ] findBySubmissionIdOrderBySentAtAsc(Long submissionId): List<Message>  :13
- [   ] findByTraineeIdOrderBySentAtAsc(Long traineeId): List<Message>  :14
- [   ] countBySubmissionId(Long submissionId): long  :15
- [   ] existsByMailMessageId(String mailMessageId): boolean  :17
- [   ] findFirstByDirectionAndMailMessageIdIn(MessageDirection direction, Collection<String> ids): Optional<Message>  :29
- [   ] countSubmissionsWithUnread(): long  :44
- [   ] countTraineesWithUnread(): long  :47

### MessageService  (src/main/java/pl/szymtrener/crm/MessageService.java)
- [pub] thread(Long submissionId): List<Message>  :70
- [pub] traineeThread(Long traineeId): List<Message>  :75
- [pub] replyTemplates(): List<ReplyTemplate>  :80
- [pub] recordSubmission(Long submissionId, String body): void  :89
- [pub] sendEmail(Long submissionId, Long traineeId, String to, String name, String body, List<AttachmentFile> attachmentFiles): SendResult  :107
- [prv] saveWithFiles(Message m, List<AttachmentFile> files): void  :157
- [prv] replyHtml(String body): String  :162
- [   ] paragraphs(String body): List<List<String>>  :171
- [pub] recordInbound(InboundMatcher.Target target, InboundMail mail, String mailMessageId): Message  :182
- [pub] attachmentsOf(List<Message> thread): Map<Long, List<MessageAttachment>>  :206
- [pub] alreadyRecorded(String mailMessageId): boolean  :211
- [pub] openSubmissionThread(Long submissionId): List<Message>  :220
- [pub] openTraineeThread(Long traineeId): List<Message>  :227
- [pub] submissionsWithUnread(Collection<Long> ids): Set<Long>  :235
- [pub] traineesWithUnread(Collection<Long> ids): Set<Long>  :240
- [pub] logPhoneCall(Long submissionId, Long traineeId, String body): Message  :246
- [pub] system(Long submissionId, Long traineeId, String body, boolean failure): void  :258
- [pub] fill(String code, String firstName, String context): String  :276
- [   ] fillBody(String body, String firstName, String context): String  :283
- [prv] dropContextSentences(String body): String  :300
- [prv] withoutSentenceAt(String line, int at): String  :313
- [prv] isSentenceEnd(char c): boolean  :328
- [prv] trimEnding(String text): String  :332
- [pub] attachToTrainee(Long submissionId, Long traineeId): void  :343
- [prv] mailEnabled(): boolean  :350

### Money  (src/main/java/pl/szymtrener/offer/Money.java)
- [pub] format(int grosze): String  :17
- [pub] amount(int grosze): String  :22

### NoteController  (src/main/java/pl/szymtrener/admin/NoteController.java)
- [pub] togglePin(@PathVariable Long id, @RequestParam String back): String  :24
- [pub] delete(@PathVariable Long id, @RequestParam String back): String  :30
- [prv] safe(String back): String  :39

### OnlineFaq  (src/main/java/pl/szymtrener/offer/OnlineFaq.java)
- [pub] answered(): boolean  :20
- [pub] getId(): Long  :24
- [pub] getQuestion(): String  :25
- [pub] setQuestion(String question): void  :26
- [pub] getAnswer(): String  :27
- [pub] setAnswer(String answer): void  :28
- [pub] getSortOrder(): int  :29
- [pub] setSortOrder(int sortOrder): void  :30
- [pub] isVisible(): boolean  :31
- [pub] setVisible(boolean visible): void  :32

### OnlineFaqRepository  (src/main/java/pl/szymtrener/offer/OnlineFaqRepository.java)
- [   ] findByVisibleTrueOrderBySortOrderAsc(): List<OnlineFaq>  :8
- [   ] findAllByOrderBySortOrderAsc(): List<OnlineFaq>  :9

### OnlineOfferService  (src/main/java/pl/szymtrener/offer/OnlineOfferService.java)
- [pub] consultation(): SimplePriceView  :52
- [pub] consultPriceGr(): int  :58
- [pub] diet(): SimplePriceView  :67
- [pub] dietPriceGr(): int  :73
- [pub] comboPriceGr(): int  :77
- [pub] updatePriceGr(): int  :81
- [prv] visibleOneOff(): List<OneOff>  :97
- [pub] oneOff(): List<OneOffView>  :112
- [pub] oneOffFrom(): String  :117
- [pub] packages(): List<PackageView>  :135
- [   ] toView(OnlinePackage p): PackageView  :140
- [   ] seatsLine(int left, int total): String  :166
- [pub] money(int grosze): String  :172
- [pub] lowestMonthly(): String  :183
- [pub] testimonials(): List<Testimonial>  :194
- [pub] faq(): List<OnlineFaq>  :203

### OnlineOfferServiceTest  (src/test/java/pl/szymtrener/offer/OnlineOfferServiceTest.java)
- [prv] pack(int seatsTaken, int seatsTotal, PricingMode mode): OnlinePackage  :14
- [   ] formatsThousands(): void  :30
- [   ] keepsStartingPriceWhileSeatsRemain(): void  :40
- [   ] switchesToTargetWhenSeatsRunOut(): void  :49
- [   ] manualTargetModeWins(): void  :58
- [   ] hidesPromotionalMarkersWhenSeatsRunOut(): void  :64
- [   ] keepsNonPromotionalBadge(): void  :81
- [   ] showsFullStartingPriceBlock(): void  :92
- [   ] seatsLineIsInflected(): void  :108
- [   ] signatureSkipsEmptyParts(): void  :118
- [   ] unansweredFaqIsHidden(): void  :134
- [   ] hiddenDietLeavesOneOffList(): void  :145
- [   ] longTestimonial(): void  :160

### OnlinePackage  (src/main/java/pl/szymtrener/offer/OnlinePackage.java)
- [pub] effectiveMode(): PricingMode  :49
- [pub] seatsLeft(): int  :55
- [pub] startingPrice(): boolean  :60
- [pub] getId(): Long  :64
- [pub] getName(): String  :65
- [pub] setName(String name): void  :66
- [pub] getDurationLabel(): String  :67
- [pub] setDurationLabel(String durationLabel): void  :68
- [pub] getCurrentTotalGr(): int  :69
- [pub] setCurrentTotalGr(int v): void  :70
- [pub] getCurrentMonthlyGr(): int  :71
- [pub] setCurrentMonthlyGr(int v): void  :72
- [pub] getTargetTotalGr(): int  :73
- [pub] setTargetTotalGr(int v): void  :74
- [pub] getTargetMonthlyGr(): int  :75
- [pub] setTargetMonthlyGr(int v): void  :76
- [pub] getPricingMode(): PricingMode  :77
- [pub] setPricingMode(PricingMode pricingMode): void  :78
- [pub] getSeatsTaken(): int  :79
- [pub] setSeatsTaken(int seatsTaken): void  :80
- [pub] getSeatsTotal(): int  :81
- [pub] setSeatsTotal(int seatsTotal): void  :82
- [pub] getBadgeText(): String  :83
- [pub] setBadgeText(String badgeText): void  :84
- [pub] isBadgeVisible(): boolean  :85
- [pub] setBadgeVisible(boolean badgeVisible): void  :86
- [pub] isBadgePromotional(): boolean  :87
- [pub] setBadgePromotional(boolean badgePromotional): void  :88
- [pub] isHighlighted(): boolean  :89
- [pub] setHighlighted(boolean highlighted): void  :90
- [pub] getSortOrder(): int  :91
- [pub] setSortOrder(int sortOrder): void  :92
- [pub] isVisible(): boolean  :93
- [pub] setVisible(boolean visible): void  :94

### OnlinePackageRepository  (src/main/java/pl/szymtrener/offer/OnlinePackageRepository.java)
- [   ] findByVisibleTrueOrderBySortOrderAsc(): List<OnlinePackage>  :8
- [   ] findAllByOrderBySortOrderAsc(): List<OnlinePackage>  :9

### OpenApiDocsIT  (src/test/java/pl/szymtrener/config/OpenApiDocsIT.java)
- [   ] docsHiddenFromAnonymous(): void  :35
- [   ] adminSeesConsentEndpoints(): void  :46

### PageView  (src/main/java/pl/szymtrener/analytics/PageView.java)
- [pub] getId(): Long  :26
- [pub] getPath(): String  :27
- [pub] setPath(String path): void  :28
- [pub] getReferrer(): String  :29
- [pub] setReferrer(String referrer): void  :30
- [pub] getSessionHash(): String  :31
- [pub] setSessionHash(String sessionHash): void  :32
- [pub] getConsentId(): Long  :33
- [pub] setConsentId(Long consentId): void  :34
- [pub] getDevice(): String  :35
- [pub] setDevice(String device): void  :36
- [pub] isBot(): boolean  :37
- [pub] setBot(boolean bot): void  :38
- [pub] getBotName(): String  :39
- [pub] setBotName(String botName): void  :40
- [pub] getViewedAt(): Instant  :41

### PageViewRepository  (src/main/java/pl/szymtrener/analytics/PageViewRepository.java)
- [   ] findByConsentIdOrderByViewedAt(Long consentId): List<PageView>  :67

### PasswordForm  (src/main/java/pl/szymtrener/admin/PasswordForm.java)
- [pub] getCurrent(): String  :8
- [pub] setCurrent(String current): void  :9
- [pub] getFresh(): String  :10
- [pub] setFresh(String fresh): void  :11
- [pub] getRepeat(): String  :12
- [pub] setRepeat(String repeat): void  :13

### Plural  (src/main/java/pl/szymtrener/common/Plural.java)
- [pub] form(long n, String one, String few, String many): String  :19
- [pub] of(long n, String one, String few, String many): String  :27
- [pub] sessions(long n): String  :31
- [pub] months(long n): String  :35
- [pub] weeks(long n): String  :39
- [pub] packages(long n): String  :43
- [pub] cancelled(long n): String  :47
- [pub] clients(long n): String  :51

### PluralTest  (src/test/java/pl/szymtrener/common/PluralTest.java)
- [   ] threeForms(): void  :16
- [   ] teensAreException(): void  :26
- [   ] otherForms(): void  :38

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
- [   ] findByContentHtmlContaining(String fragment): List<Post>  :14
- [   ] findBySlug(String slug): Optional<Post>  :22
- [   ] findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable): Page<Post>  :25
- [   ] findByStatusAndCategorySlugOrderByPublishedAtDesc(PostStatus status, String categorySlug, Pageable pageable): Page<Post>  :28
- [   ] findBySlugAndStatus(String slug, PostStatus status): Optional<Post>  :31
- [   ] findByStatusAndPublishAtLessThanEqual(PostStatus status, Instant now): List<Post>  :37
- [   ] findTop3ByStatusAndCategoryIdAndIdNotOrderByPublishedAtDesc(PostStatus status, Long categoryId, Long excludedId): List<Post>  :40
- [   ] findTop3ByStatusAndIdNotOrderByPublishedAtDesc(PostStatus status, Long excludedId): List<Post>  :43
- [   ] findTop6ByStatusAndIdNotOrderByPublishedAtDesc(PostStatus status, Long excludedId): List<Post>  :47
- [   ] findSlugsForSitemap(): List<Object[]>  :68
- [   ] findTop20ByStatusOrderByPublishedAtDesc(PostStatus status): List<Post>  :72
- [   ] countByStatus(PostStatus status): long  :78
- [   ] findAllByOrderByUpdatedAtDesc(Pageable pageable): Page<Post>  :84
- [   ] findByStatusOrderByUpdatedAtDesc(PostStatus status, Pageable pageable): Page<Post>  :98
- [   ] findTop5ByStatusOrderByPublishAtAsc(PostStatus status): List<Post>  :116
- [   ] findTop5ByStatusOrderByUpdatedAtDesc(PostStatus status): List<Post>  :120

### PostService  (src/main/java/pl/szymtrener/content/PostService.java)
- [pub] published(String categorySlug, int page, int size): Page<PostView>  :59
- [pub] requirePublished(String slug): Post  :68
- [pub] related(Post post): List<PostView>  :79
- [pub] search(String query, int page, int size): Page<PostView>  :99
- [pub] registerView(Long postId): void  :107
- [pub] toFullView(Post p): PostView  :113
- [pub] toCardView(Post p): PostView  :135
- [pub] save(Post post): Post  :152
- [pub] save(Post post, String desiredSlug): Post  :162
- [pub] replaceContentKeepingDates(Long postId, String html): void  :210
- [prv] syncMediaLinks(Post post): void  :215
- [prv] mediaIdFromUrl(String url): Optional<Long>  :245
- [pub] uniqueSlug(String base, Long selfId): String  :267
- [pub] postsUsing(Long mediaId): List<String>  :282
- [pub] deleteOrArchive(Post post): boolean  :293
- [pub] categories(): List<Category>  :305
- [   ] iso(Instant instant): String  :307
- [   ] label(Instant instant): String  :311

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

### PricingMode  (src/main/java/pl/szymtrener/offer/PricingMode.java)
- [pub] label(): String  :13

### PublicFormController  (src/main/java/pl/szymtrener/web/PublicFormController.java)
- [pub] online(@Valid @RequestBody FormRequests.OnlineForm form, BindingResult errors, HttpServletRequest request): ResponseEntity<?>  :29
- [pub] contact(@Valid @RequestBody FormRequests.ContactForm form, BindingResult errors, HttpServletRequest request): ResponseEntity<?>  :38
- [prv] reject(BindingResult errors, Boolean honeypot, HttpServletRequest request): ResponseEntity<?>  :46
- [prv] ip(HttpServletRequest request): String  :63

### PublicFormControllerTest  (src/test/java/pl/szymtrener/web/PublicFormControllerTest.java)
- [   ] allowByDefault(): void  :62
- [prv] validContact(): Map<String, Object>  :66
- [prv] validOnline(): Map<String, Object>  :77
- [   ] acceptsValidContactForm(): void  :93
- [   ] acceptsValidOnlineForm(): void  :105
- [   ] rejectsRequestWithoutCsrfToken(): void  :117
- [   ] honeypotSilentlyDropsBotSubmissions(): void  :128
- [   ] honeypotWinsOverValidation(): void  :144
- [   ] rejectsWhenRateLimited(): void  :160
- [   ] consentIsNotRequired(): void  :175
- [   ] reportsFieldValidationErrors(): void  :189
- [   ] onlineFormRequiresItsOwnFields(): void  :206
- [   ] rateLimitIsCheckedBeforeValidation(): void  :223

### PublishScheduler  (src/main/java/pl/szymtrener/scheduler/PublishScheduler.java)
- [pub] publishDue(): void  :32

### RateLimiter  (src/main/java/pl/szymtrener/submission/RateLimiter.java)
- [pub] allow(String key): boolean  :37

### ReminderScheduler  (src/main/java/pl/szymtrener/submission/ReminderScheduler.java)
- [pub] sendDueReminders(): void  :45
- [pub] due(): List<Submission>  :71
- [prv] body(List<Submission> due): String  :75

### ReplyMailTest  (src/test/java/pl/szymtrener/crm/ReplyMailTest.java)
- [   ] fillsContext(): void  :35
- [   ] dropsLineWithEmptyContext(): void  :44
- [   ] dropsOnlyContextSentence(): void  :60
- [   ] noNameAndManualPlaceholders(): void  :74
- [   ] splitsParagraphs(): void  :83
- [   ] rendersEscapedReply(): void  :90

### ReplySendTest  (src/test/java/pl/szymtrener/crm/ReplySendTest.java)
- [   ] sendsMultipartReply(): void  :36
- [prv] isType(Part part, String type): boolean  :79
- [prv] collect(Part part, List<Part> leaves): void  :87
- [prv] engine(): SpringTemplateEngine  :95

### ReplyTemplate  (src/main/java/pl/szymtrener/crm/ReplyTemplate.java)
- [pub] getId(): Long  :21
- [pub] getCode(): String  :22
- [pub] setCode(String code): void  :23
- [pub] getLabel(): String  :24
- [pub] setLabel(String label): void  :25
- [pub] getBody(): String  :26
- [pub] setBody(String body): void  :27
- [pub] getSortOrder(): int  :28
- [pub] setSortOrder(int sortOrder): void  :29

### ReplyTemplateMigrationIT  (src/test/java/pl/szymtrener/crm/ReplyTemplateMigrationIT.java)
- [   ] allSeededTemplatesUpdated(): void  :28
- [   ] keepsTrainerEdits(): void  :41

### ReplyTemplateRepository  (src/main/java/pl/szymtrener/crm/ReplyTemplateRepository.java)
- [   ] findAllByOrderBySortOrderAsc(): List<ReplyTemplate>  :9
- [   ] findByCode(String code): Optional<ReplyTemplate>  :10

### SecurityConfig  (src/main/java/pl/szymtrener/config/SecurityConfig.java)
- [   ] filterChain(HttpSecurity http): SecurityFilterChain  :16
- [   ] passwordEncoder(): PasswordEncoder  :57

### SeoController  (src/main/java/pl/szymtrener/seo/SeoController.java)
- [pub] robots(): String  :44
- [pub] sitemap(): String  :90
- [prv] url(StringBuilder xml, String location, Instant lastMod, String priority): void  :106
- [pub] feed(): String  :120
- [prv] rfc822(Instant instant): String  :159
- [prv] escape(String text): String  :163
- [pub] indexNowKey(@PathVariable String key): String  :170
- [pub] llms(): String  :180

### SeoScoreService  (src/main/java/pl/szymtrener/seo/SeoScoreService.java)
- [pub] pending(): List<Check>  :41
- [pub] evaluate(String contentHtml, String seoTitle, String seoDesc, String coverAlt): Result  :52
- [pub] ofPublication(String publicationHtml, String seoTitle, String seoDesc, String coverAlt): Result  :91
- [prv] hasImage(Document doc): boolean  :96
- [prv] allImagesHaveAlt(Document doc): boolean  :104
- [prv] label(int score): String  :110
- [prv] hint(long done, int total): String  :116
- [prv] trim(String value): String  :121

### SessionStatus  (src/main/java/pl/szymtrener/crm/SessionStatus.java)
- [pub] label(): String  :13
- [pub] badge(): String  :16

### SettingsService  (src/main/java/pl/szymtrener/settings/SettingsService.java)
- [pub] get(String key, String fallback): String  :68
- [pub] getInt(String key, int fallback): int  :74
- [pub] getLong(String key, long fallback): long  :85
- [pub] getBoolean(String key, boolean fallback): boolean  :96
- [pub] set(String key, String value): void  :102
- [pub] all(): Map<String, String>  :112
- [prv] ensureLoaded(): void  :117

### SlugUtil  (src/main/java/pl/szymtrener/common/SlugUtil.java)
- [pub] slugify(String input): String  :10

### SlugUtilTest  (src/test/java/pl/szymtrener/common/SlugUtilTest.java)
- [   ] slugifiesPolishText(String input, String expected): void  :26
- [   ] handlesEmptyInput(String input): void  :34
- [   ] trimsLongTitles(): void  :40
- [   ] handlesPunctuationOnlyTitle(): void  :53
- [   ] isDeterministic(): void  :59

### StationaryKind  (src/main/java/pl/szymtrener/offer/StationaryKind.java)
- [pub] label(): String  :13

### StationaryOfferService  (src/main/java/pl/szymtrener/offer/StationaryOfferService.java)
- [pub] hasCare(): boolean  :88
- [pub] page(): Page  :94
- [prv] ofKind(List<StationaryPackage> all, StationaryKind kind): List<StationaryPackage>  :111
- [prv] views(List<StationaryPackage> found): List<PackageView>  :115
- [prv] totalLine(StationaryPackage p): String  :142
- [prv] termShort(StationaryPackage p): String  :149
- [   ] termLabel(Integer months): String  :156
- [   ] validityLabel(Integer weeks): String  :162
- [prv] biggestPack(List<StationaryPackage> found): Optional<StationaryPackage>  :168
- [prv] yearCompare(List<StationaryPackage> found): YearCompare  :178
- [   ] yearCompare(StationaryPackage care, StationaryPackage pack): YearCompare  :186
- [   ] inSessions(int differenceGr, int perSessionGr): String  :207
- [prv] rules(): List<String>  :218
- [   ] of(List<StationaryPackage> found): KindPrices  :233
- [   ] lowest(): Integer  :243
- [prv] priceSentence(KindPrices one, KindPrices pair): String  :252
- [   ] careTerms(StationaryPackage care): String  :280
- [   ] locative(String name): String  :293
- [prv] validitySentence(List<StationaryPackage> one): String  :301
- [prv] pairPerPersonSentence(List<StationaryPackage> pair): String  :320
- [prv] priceRange(List<StationaryPackage> visible): String  :328
- [prv] format(Integer gr): String  :337
- [prv] blankToNull(String s): String  :341

### StationaryOfferServiceTest  (src/test/java/pl/szymtrener/offer/StationaryOfferServiceTest.java)
- [prv] pack(int sessions, int priceGr, Integer weeks): StationaryPackage  :14
- [   ] discountAgainstSingleEntry(): void  :25
- [   ] noDiscountWhenNotCheaper(): void  :35
- [   ] totalIsComputed(): void  :44
- [   ] validityIsInflected(): void  :52
- [   ] singleEntryHasNoValidity(): void  :63
- [   ] amountWithoutUnit(): void  :72
- [prv] care(int perSessionGr): StationaryPackage  :78
- [   ] discountsMatchOctoberPriceList(): void  :87
- [   ] yearCompareFromPrices(): void  :100
- [   ] noYearCompareWhenCareIsNotCheaper(): void  :115
- [   ] differenceInSessions(): void  :121
- [   ] termLabel(): void  :131
- [   ] careTermsFromData(): void  :139
- [   ] monthlyFollowsTerm(): void  :151
- [   ] locative(): void  :158
- [   ] yearCompareSurvivesOverflow(): void  :165
- [prv] serviceWith(StationaryPackage... all): StationaryOfferService  :171
- [prv] of(StationaryKind kind, StationaryPackage p): StationaryPackage  :177
- [   ] pairSentenceNeedsCheaperOption(): void  :184
- [   ] careOnlyForPairsCountsAsCare(): void  :193
- [   ] yearCompareUsesSameCareAsSentence(): void  :202

### StationaryPackage  (src/main/java/pl/szymtrener/offer/StationaryPackage.java)
- [pub] totalGr(): int  :47
- [pub] single(): boolean  :53
- [pub] discountPercent(int singlePriceGr): int  :62
- [pub] getId(): Long  :67
- [pub] getKind(): StationaryKind  :68
- [pub] setKind(StationaryKind kind): void  :69
- [pub] getName(): String  :70
- [pub] setName(String name): void  :71
- [pub] getSessions(): int  :72
- [pub] setSessions(int sessions): void  :73
- [pub] getPricePerSessionGr(): int  :74
- [pub] setPricePerSessionGr(int v): void  :75
- [pub] getValidityWeeks(): Integer  :76
- [pub] setValidityWeeks(Integer validityWeeks): void  :77
- [pub] isFeatured(): boolean  :78
- [pub] setFeatured(boolean featured): void  :79
- [pub] getSortOrder(): int  :80
- [pub] setSortOrder(int sortOrder): void  :81
- [pub] isVisible(): boolean  :82
- [pub] setVisible(boolean visible): void  :83
- [pub] isMonthly(): boolean  :85
- [pub] getTermMonths(): Integer  :86
- [pub] setTermMonths(Integer termMonths): void  :87
- [pub] getBadgeText(): String  :88
- [pub] setBadgeText(String badgeText): void  :89
- [pub] getDescription(): String  :90
- [pub] setDescription(String description): void  :91

### StationaryPackageRepository  (src/main/java/pl/szymtrener/offer/StationaryPackageRepository.java)
- [   ] findAllByOrderByKindAscSortOrderAsc(): List<StationaryPackage>  :8

### Submission  (src/main/java/pl/szymtrener/submission/Submission.java)
- [pub] getId(): Long  :61
- [pub] getType(): SubmissionType  :62
- [pub] setType(SubmissionType type): void  :63
- [pub] getName(): String  :64
- [pub] setName(String name): void  :65
- [pub] getEmail(): String  :66
- [pub] setEmail(String email): void  :67
- [pub] getPhone(): String  :68
- [pub] setPhone(String phone): void  :69
- [pub] getCity(): String  :70
- [pub] setCity(String city): void  :71
- [pub] getCurrentTraining(): String  :72
- [pub] setCurrentTraining(String currentTraining): void  :73
- [pub] getGoal(): String  :74
- [pub] setGoal(String goal): void  :75
- [pub] getEquipment(): String  :76
- [pub] setEquipment(String equipment): void  :77
- [pub] getSource(): String  :78
- [pub] setSource(String source): void  :79
- [pub] getOfferPath(): String  :80
- [pub] setOfferPath(String offerPath): void  :81
- [pub] getOfferPackage(): String  :82
- [pub] setOfferPackage(String offerPackage): void  :83
- [pub] getInterest(): String  :84
- [pub] setInterest(String interest): void  :85
- [pub] getMessage(): String  :86
- [pub] setMessage(String message): void  :87
- [pub] getConsentAt(): Instant  :88
- [pub] getStatus(): SubmissionStatus  :89
- [pub] setStatus(SubmissionStatus status): void  :90
- [pub] getCallAt(): Instant  :91
- [pub] setCallAt(Instant callAt): void  :92
- [pub] getIpHash(): String  :93
- [pub] setIpHash(String ipHash): void  :94
- [pub] getUserAgent(): String  :95
- [pub] setUserAgent(String userAgent): void  :96
- [pub] isMailSent(): boolean  :97
- [pub] setMailSent(boolean mailSent): void  :98
- [pub] getMailError(): String  :99
- [pub] setMailError(String mailError): void  :100
- [pub] getContactedAt(): Instant  :101
- [pub] setContactedAt(Instant v): void  :102
- [pub] getCallBookedAt(): Instant  :103
- [pub] setCallBookedAt(Instant v): void  :104
- [pub] getConvertedAt(): Instant  :105
- [pub] setConvertedAt(Instant v): void  :106
- [pub] getArchivedAt(): Instant  :107
- [pub] setArchivedAt(Instant v): void  :108
- [pub] getRemindAt(): Instant  :109
- [pub] setRemindAt(Instant remindAt): void  :110
- [pub] isRemindDone(): boolean  :111
- [pub] setRemindDone(boolean remindDone): void  :112
- [pub] getCreatedAt(): Instant  :113
- [pub] offerContext(): String  :120
- [pub] stageDate(String stage): String  :135
- [pub] callAtLocal(): String  :152
- [pub] initials(): String  :160
- [pub] firstName(): String  :171
- [pub] greeting(): String  :179
- [pub] getCreatedLabel(): String  :186
- [pub] getConsentLabel(): String  :192
- [prv] label(Instant at): String  :196

### SubmissionNote  (src/main/java/pl/szymtrener/submission/SubmissionNote.java)
- [pub] getId(): Long  :30
- [pub] getSubmissionId(): Long  :31
- [pub] setSubmissionId(Long submissionId): void  :32
- [pub] getTraineeId(): Long  :33
- [pub] setTraineeId(Long traineeId): void  :34
- [pub] isPinned(): boolean  :35
- [pub] setPinned(boolean pinned): void  :36
- [pub] getTags(): String  :37
- [pub] setTags(String tags): void  :38
- [pub] getAuthor(): String  :39
- [pub] setAuthor(String author): void  :40
- [pub] getBody(): String  :41
- [pub] setBody(String body): void  :42
- [pub] getCreatedAt(): Instant  :43
- [pub] tagList(): java.util.List<String>  :47
- [pub] warning(): boolean  :58
- [pub] getCreatedLabel(): String  :63

### SubmissionNoteRepository  (src/main/java/pl/szymtrener/submission/SubmissionNoteRepository.java)
- [   ] findBySubmissionIdOrderByPinnedDescCreatedAtDesc(Long submissionId): List<SubmissionNote>  :9
- [   ] findByTraineeIdOrderByPinnedDescCreatedAtDesc(Long traineeId): List<SubmissionNote>  :11
- [   ] findBySubmissionIdOrderByCreatedAtDesc(Long submissionId): List<SubmissionNote>  :13

### SubmissionRepository  (src/main/java/pl/szymtrener/submission/SubmissionRepository.java)
- [   ] findAllByOrderByCreatedAtDesc(Pageable pageable): Page<Submission>  :10
- [   ] findByStatusOrderByCreatedAtDesc(SubmissionStatus status, Pageable pageable): Page<Submission>  :11
- [   ] findByTypeOrderByCreatedAtDesc(SubmissionType type, Pageable pageable): Page<Submission>  :12
- [   ] findTop5ByOrderByCreatedAtDesc(): List<Submission>  :13
- [   ] countByStatus(SubmissionStatus status): long  :14
- [   ] countByCreatedAtAfter(Instant since): long  :15
- [   ] findByRemindDoneFalseAndRemindAtLessThanEqual(java.time.Instant at): java.util.List<Submission>  :18

### SubmissionService  (src/main/java/pl/szymtrener/submission/SubmissionService.java)
- [pub] acceptOnline(FormRequests.OnlineForm form, String ip, String userAgent): Submission  :33
- [pub] acceptContact(FormRequests.ContactForm form, String ip, String userAgent): Submission  :50
- [prv] persist(Submission s, String ip, String userAgent): Submission  :61
- [prv] opening(Submission s): String  :76
- [pub] changeStatus(Long id, SubmissionStatus status, Instant callAt): void  :91
- [pub] stage(Long id, SubmissionStatus status): Submission  :104
- [prv] stamp(Submission s, SubmissionStatus status): void  :113
- [pub] remind(Long id, Instant at): void  :129
- [pub] remindDone(Long id): void  :138
- [pub] delete(Long id): void  :150
- [pub] export(Long id): Map<String, Object>  :159
- [pub] addNote(Long submissionId, String author, String body): void  :196
- [pub] addNote(Long submissionId, Long traineeId, String author, String body, String tags): void  :201
- [pub] togglePin(Long noteId): boolean  :213
- [pub] deleteNote(Long noteId): void  :222
- [prv] hash(String ip): String  :226

### SubmissionStatus  (src/main/java/pl/szymtrener/submission/SubmissionStatus.java)
- [pub] label(): String  :6
- [pub] badge(): String  :17

### SubmissionType  (src/main/java/pl/szymtrener/submission/SubmissionType.java)
- [pub] label(): String  :13

### SzymtrenerApplication  (src/main/java/pl/szymtrener/SzymtrenerApplication.java)
- [pub] main(String[] args): void  :14

### Testimonial  (src/main/java/pl/szymtrener/offer/Testimonial.java)
- [pub] signature(): String  :31
- [pub] isLong(): boolean  :47
- [pub] initial(): String  :52
- [pub] getId(): Long  :56
- [pub] getName(): String  :57
- [pub] setName(String name): void  :58
- [pub] getCity(): String  :59
- [pub] setCity(String city): void  :60
- [pub] getCooperationFormat(): String  :61
- [pub] setCooperationFormat(String v): void  :62
- [pub] getDurationLabel(): String  :63
- [pub] setDurationLabel(String v): void  :64
- [pub] getBody(): String  :65
- [pub] setBody(String body): void  :66
- [pub] getMediaId(): Long  :67
- [pub] setMediaId(Long mediaId): void  :68
- [pub] getSortOrder(): int  :69
- [pub] setSortOrder(int sortOrder): void  :70
- [pub] isVisible(): boolean  :71
- [pub] setVisible(boolean visible): void  :72

### TestimonialRepository  (src/main/java/pl/szymtrener/offer/TestimonialRepository.java)
- [   ] findByVisibleTrueOrderBySortOrderAsc(): List<Testimonial>  :8
- [   ] findAllByOrderBySortOrderAsc(): List<Testimonial>  :9

### ThreadPagesRenderIT  (src/test/java/pl/szymtrener/admin/ThreadPagesRenderIT.java)
- [   ] threadPagesRenderCompletely(): void  :35

### ThumbnailFetcher  (src/main/java/pl/szymtrener/media/ThumbnailFetcher.java)
- [   ] fetch(String videoId): Optional<byte[]>  :9

### Trainee  (src/main/java/pl/szymtrener/crm/Trainee.java)
- [pub] getId(): Long  :60
- [pub] getSubmissionId(): Long  :61
- [pub] setSubmissionId(Long submissionId): void  :62
- [pub] getName(): String  :63
- [pub] setName(String name): void  :64
- [pub] getCity(): String  :65
- [pub] setCity(String city): void  :66
- [pub] getAge(): Integer  :67
- [pub] setAge(Integer age): void  :68
- [pub] getMode(): TraineeMode  :69
- [pub] setMode(TraineeMode mode): void  :70
- [pub] getStartedAt(): LocalDate  :71
- [pub] setStartedAt(LocalDate startedAt): void  :72
- [pub] getPlanName(): String  :73
- [pub] setPlanName(String planName): void  :74
- [pub] getSessionCount(): int  :75
- [pub] setSessionCount(int sessionCount): void  :76
- [pub] getStatus(): TraineeStatus  :77
- [pub] setStatus(TraineeStatus status): void  :78
- [pub] getCreatedAt(): Instant  :79
- [pub] getEmail(): String  :80
- [pub] setEmail(String email): void  :81
- [pub] getPhone(): String  :82
- [pub] setPhone(String phone): void  :83
- [pub] getFixedSlots(): String  :84
- [pub] setFixedSlots(String fixedSlots): void  :85
- [pub] getLastContactAt(): Instant  :86
- [pub] setLastContactAt(Instant lastContactAt): void  :87
- [pub] getSource(): String  :88
- [pub] setSource(String source): void  :89
- [pub] getGoalNote(): String  :90
- [pub] setGoalNote(String goalNote): void  :91
- [pub] initials(): String  :95
- [pub] daysSinceContact(): long  :109
- [pub] lastContactLabel(): String  :116
- [pub] startedLabel(): String  :126
- [pub] weeksTogether(): Long  :134

### TraineeForm  (src/main/java/pl/szymtrener/crm/TraineeForm.java)
- [pub] getId(): Long  :22
- [pub] setId(Long id): void  :23
- [pub] getSubmissionId(): Long  :24
- [pub] setSubmissionId(Long submissionId): void  :25
- [pub] getName(): String  :26
- [pub] setName(String name): void  :27
- [pub] getCity(): String  :28
- [pub] setCity(String city): void  :29
- [pub] getAge(): Integer  :30
- [pub] setAge(Integer age): void  :31
- [pub] getMode(): TraineeMode  :32
- [pub] setMode(TraineeMode mode): void  :33
- [pub] getStartedAt(): LocalDate  :34
- [pub] setStartedAt(LocalDate startedAt): void  :35
- [pub] getPlanName(): String  :36
- [pub] setPlanName(String planName): void  :37
- [pub] getSessionCount(): int  :38
- [pub] setSessionCount(int sessionCount): void  :39
- [pub] getStatus(): TraineeStatus  :40
- [pub] setStatus(TraineeStatus status): void  :41
- [pub] getEmail(): String  :42
- [pub] setEmail(String email): void  :43
- [pub] getPhone(): String  :44
- [pub] setPhone(String phone): void  :45
- [pub] getFixedSlots(): String  :46
- [pub] setFixedSlots(String fixedSlots): void  :47
- [pub] getSource(): String  :48
- [pub] setSource(String source): void  :49
- [pub] getGoalNote(): String  :50
- [pub] setGoalNote(String goalNote): void  :51

### TraineeMode  (src/main/java/pl/szymtrener/crm/TraineeMode.java)
- [pub] label(): String  :6

### TraineeRepository  (src/main/java/pl/szymtrener/crm/TraineeRepository.java)
- [   ] findAllOrdered(Pageable pageable): Page<Trainee>  :18
- [   ] countByStatus(TraineeStatus status): long  :23
- [   ] findBySubmissionId(Long submissionId): Optional<Trainee>  :25
- [   ] findFirstByEmailIgnoreCaseOrderByIdDesc(String email): Optional<Trainee>  :28
- [   ] countByMode(TraineeMode mode): long  :30

### TraineeService  (src/main/java/pl/szymtrener/crm/TraineeService.java)
- [pub] fromSubmission(Long submissionId): Trainee  :36
- [pub] save(TraineeForm form): Trainee  :66
- [pub] delete(Long id): void  :90
- [prv] blankToNull(String value): String  :94

### TraineeStatus  (src/main/java/pl/szymtrener/crm/TraineeStatus.java)
- [pub] label(): String  :6
- [pub] badge(): String  :15

### TrainingPackage  (src/main/java/pl/szymtrener/crm/TrainingPackage.java)
- [pub] valueGr(): int  :23
- [pub] getId(): Long  :27
- [pub] getTraineeId(): Long  :28
- [pub] setTraineeId(Long traineeId): void  :29
- [pub] getName(): String  :30
- [pub] setName(String name): void  :31
- [pub] getTotalSessions(): int  :32
- [pub] setTotalSessions(int totalSessions): void  :33
- [pub] getPricePerSessionGr(): int  :34
- [pub] setPricePerSessionGr(int v): void  :35
- [pub] getPurchasedAt(): LocalDate  :36
- [pub] setPurchasedAt(LocalDate purchasedAt): void  :37
- [pub] isActive(): boolean  :38
- [pub] setActive(boolean active): void  :39

### TrainingPackageRepository  (src/main/java/pl/szymtrener/crm/TrainingPackageRepository.java)
- [   ] findByTraineeIdOrderByPurchasedAtDesc(Long traineeId): List<TrainingPackage>  :9
- [   ] findByTraineeIdAndActiveTrue(Long traineeId): List<TrainingPackage>  :10
- [   ] findByPurchasedAtGreaterThanEqual(LocalDate from): List<TrainingPackage>  :11

### TrainingSession  (src/main/java/pl/szymtrener/crm/TrainingSession.java)
- [pub] dayLabel(): String  :37
- [pub] monthLabel(): String  :43
- [pub] whenLabel(): String  :50
- [pub] shortLabel(): String  :57
- [pub] startsAtLocal(): String  :64
- [pub] getId(): Long  :68
- [pub] getTraineeId(): Long  :69
- [pub] setTraineeId(Long traineeId): void  :70
- [pub] getPackageId(): Long  :71
- [pub] setPackageId(Long packageId): void  :72
- [pub] getStartsAt(): Instant  :73
- [pub] setStartsAt(Instant startsAt): void  :74
- [pub] getTitle(): String  :75
- [pub] setTitle(String title): void  :76
- [pub] getNote(): String  :77
- [pub] setNote(String note): void  :78
- [pub] getStatus(): SessionStatus  :79
- [pub] setStatus(SessionStatus status): void  :80
- [pub] isConsumesPackage(): boolean  :81
- [pub] setConsumesPackage(boolean consumesPackage): void  :82

### TrainingSessionRepository  (src/main/java/pl/szymtrener/crm/TrainingSessionRepository.java)
- [   ] findByTraineeIdOrderByStartsAtDesc(Long traineeId): List<TrainingSession>  :9
- [   ] findByTraineeIdAndStatusOrderByStartsAtAsc(Long traineeId, SessionStatus status): List<TrainingSession>  :10
- [   ] findByStartsAtBetweenOrderByStartsAtAsc(Instant from, Instant to): List<TrainingSession>  :11

### YoutubeThumbnailBackfill  (src/main/java/pl/szymtrener/content/YoutubeThumbnailBackfill.java)
- [pub] onReady(): void  :36
- [   ] localizeLegacyPosts(): int  :41

### YoutubeThumbnailBackfillIT  (src/test/java/pl/szymtrener/content/YoutubeThumbnailBackfillIT.java)
- [   ] rewritesLegacyPost(): void  :46

### YoutubeThumbnailBackfillTest  (src/test/java/pl/szymtrener/content/YoutubeThumbnailBackfillTest.java)
- [prv] legacyPost(String html): Post  :27
- [   ] rewritesLegacyPosts(): void  :36
- [   ] keepsPostWhenThumbnailUnavailable(): void  :46
- [   ] skipsPostsWithoutThumbnails(): void  :56
- [   ] survivesFailures(): void  :67

### YoutubeThumbnails  (src/main/java/pl/szymtrener/media/YoutubeThumbnails.java)
- [pub] localize(String html): String  :37
- [pub] localizeAllOrNothing(String html): Optional<String>  :46
- [prv] rewrite(String html, boolean dropFailed): Optional<String>  :52
- [prv] localUrl(String videoId): Optional<String>  :71

### YoutubeThumbnailsTest  (src/test/java/pl/szymtrener/media/YoutubeThumbnailsTest.java)
- [   ] setUp(): void  :31
- [prv] stored(String key): MediaFile  :36
- [   ] fetchesOnceAndRewrites(): void  :45
- [   ] reusesLibrary(): void  :56
- [   ] dropsImageWhenFetchFails(): void  :65
- [   ] rejectsUnexpectedSource(): void  :75
- [   ] leavesOtherContentAlone(): void  :84
- [   ] fetcherAcceptsOnlySmallJpeg(): void  :92
- [   ] allOrNothingKeepsContentOnFailure(): void  :104
- [   ] acceptsQueryString(): void  :112
- [   ] textOnlyMentionIsUnchanged(): void  :123

