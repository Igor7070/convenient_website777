package com.example.unl_pos12.controller.job_search;

import com.example.unl_pos12.model.job_search.Vacancy;
import com.example.unl_pos12.model.job_search.resume.ResumeSendRequest;
import com.example.unl_pos12.repo.VacancyRepository;
import com.example.unl_pos12.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/convenient_job_search/readyResume")
public class ResumeRestController {

    @Autowired
    private RestTemplate customRestTemplate;
    @Autowired
    private VacancyRepository vacancyRepository;
    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;
    @Autowired
    private EmailService emailService;
    private static int countRequestWorkUa = 0;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadResume(@RequestParam("vacancyId") Long vacancyId,
                                               @RequestParam("resumeFile") String resumeFile,
                                               HttpSession session) {
        String accessToken = "";
        String email = "";
        String firstName = "";
        String lastName = "";
        String submitPageUrl = "";
        EmailRequest emailRequest = new EmailRequest();
        try {
            // Получение текущей аутентификации
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // Получение токена, маил, firstName и lastName
            accessToken = extractGoogleAccessToken(authentication);
            email = extractEmail(authentication);
            firstName = extractFirstName(authentication);
            lastName = extractLastName(authentication);

            // Проверка, был ли токен получен
            if (accessToken == null) {
                session.setAttribute("message", "Ошибка: не удалось получить токен доступа. Вам необходимо авторизироваться.");
                // Перенаправление на страницу с успешным сообщением
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/convenient_job_search/readyResume/sent?vacancyId=" + vacancyId))
                        .build();
            }
            System.out.println("accessToken: " + accessToken);
            System.out.println("eMail: " + email);
            System.out.println("firstName: " + firstName);
            System.out.println("lastName: " + lastName);

            // Путь к файлу резюме в папке static/resume
            System.out.println("Получен POST-запрос на загрузку резюме");
            String filePath = "src/main/resources/static/resumes/" + resumeFile; // Укажите имя файла
            //Path filePath = Paths.get("src/main/resources/static/resumes").resolve(resumeFile).normalize();
            //System.out.println(filePath);
            File file = new File(filePath);
            FileSystemResource resource = new FileSystemResource(file);

            Optional<Vacancy> vacancyOpt = vacancyRepository.findById(Math.toIntExact(vacancyId));
            ArrayList<Vacancy> res = new ArrayList<>();
            vacancyOpt.ifPresent(res::add);
            if (res.isEmpty()) {
                // Сохранение сообщения об ошибке в сессии
                session.setAttribute("message", "Удалена история поиска вакансий и данная вакансия недоступна теперь. Попробуйте снова.");
                // Перенаправление на страницу с успешным сообщением
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/convenient_job_search/readyResume/sent?vacancyId=" + vacancyId))
                        .build();
            }
            Vacancy vacancy = res.get(0);
            System.out.println(vacancy.getId());
            System.out.println(vacancy.getTitle());
            System.out.println(vacancy.getCompanyName());
            System.out.println(vacancy.getCity());
            System.out.println(vacancy.getSiteName());
            System.out.println(vacancy.getUrl());
            submitPageUrl = pageForSendingResume(vacancy); // URL страницы где происходит отправка резюме
            System.out.println("submitPageUrl: " + submitPageUrl);

            if (vacancy.getSiteName().contains("robota.ua")) {
                System.out.println("Site is robota.ua");
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String encodedFile = Base64.getEncoder().encodeToString(fileBytes);

                String targetProxyLoadSendUrl = "https://unlimitedpossibilities12.org/api/proxy/upload-send-resume-rabota-ua";
                String vacancyIdRabotaUa = extractIdVacancyRabotaUa(vacancy.getUrl());
                long vacancyIdRabotaUaLong = Long.parseLong(vacancyIdRabotaUa);
                String targetLoadSendUrl = "https://apply-api.robota.ua/attach-application";

                // Создание объекта запроса для прокси
                ProxyRequest proxyRequest = new ProxyRequest(accessToken, filePath,
                        vacancyIdRabotaUaLong, email, firstName, lastName, encodedFile,
                        targetLoadSendUrl, submitPageUrl);

                // Отправка POST-запросов через прокси
                ResponseEntity<String> responseLoadAndSend = customRestTemplate.postForEntity(targetProxyLoadSendUrl, proxyRequest, String.class);

                // Проверка ответа
                if (responseLoadAndSend.getStatusCode() == HttpStatus.OK) {
                    System.out.println("Resume successfully sent");
                } else {
                    System.out.println("Error sending resume: " + responseLoadAndSend.getStatusCode() + " - " + responseLoadAndSend.getBody());
                    session.setAttribute("message", "Ошибка отправки резюме: " + responseLoadAndSend.getStatusCode() + " - " + responseLoadAndSend.getBody());
                    session.setAttribute("submitPageUrl", submitPageUrl); // Сохраняем targetUrl в сессии
                    emailRequest.setTo(email);
                    emailRequest.setSubject("Отправка резюме");
                    emailRequest.setBody("Вынуждены вас огорчить, ваше резюме не доставлено работодателю. Попробуйте снова.");
                    sendEmail(emailRequest, accessToken, resumeFile);
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create("/convenient_job_search/readyResume/sent?vacancyId=" + vacancyId))
                            .build();
                }
                // Перенаправление на страницу с успешным сообщением
                session.setAttribute("message", "Ваше резюме успешно отправлено!");
                session.setAttribute("submitPageUrl", submitPageUrl); // Сохраняем targetUrl в сессии
                emailRequest.setTo(email);
                emailRequest.setSubject("Отправка резюме");
                emailRequest.setBody("Поздравляем, ваше резюме успешно отправлено в компанию " + vacancy.getCompanyName() + "! Успешного отклика и дальнейшего поднятия бабла!");
                sendEmail(emailRequest, accessToken, resumeFile);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/convenient_job_search/readyResume/sent?vacancyId=" + vacancyId))
                        .build();
            }

            System.out.println("Site is work.ua");

            //Без прокси, отправка на URL страницы где загрузка и отправка резюме
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("resume", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Отправка POST-запроса
            ResponseEntity<String> response = customRestTemplate.postForEntity(submitPageUrl, requestEntity, String.class);
            countRequestWorkUa++;

            // Проверка ответа
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Resume successfully sent");
            } else {
                //из за того что самый первый запрос всегда с ошибкой 302
                if (countRequestWorkUa == 1) {
                    response = customRestTemplate.postForEntity(submitPageUrl, requestEntity, String.class);
                    if (response.getStatusCode() == HttpStatus.OK) {
                        System.out.println("Resume successfully sent");
                        session.setAttribute("message", "Ваше резюме успешно отправлено!");
                        session.setAttribute("submitPageUrl", submitPageUrl);
                        emailRequest.setTo(email);
                        emailRequest.setSubject("Отправка резюме");
                        emailRequest.setBody("Поздравляем, ваше резюме успешно отправлено в компанию " + vacancy.getCompanyName() + "! Успешного отклика и дальнейшего поднятия бабла!");
                        sendEmail(emailRequest, accessToken, resumeFile);
                        return ResponseEntity.status(HttpStatus.FOUND)
                                .location(URI.create("/convenient_job_search/readyResume/sent?vacancyId=" + vacancyId))
                                .build();
                    }
                }
                System.out.println("Error sending resume: " + response.getStatusCode() + " - " + response.getBody());
                session.setAttribute("message", "Ошибка отправки резюме: " + response.getStatusCode() + " - " + response.getBody());
                session.setAttribute("submitPageUrl", submitPageUrl);
                emailRequest.setTo(email);
                emailRequest.setSubject("Отправка резюме");
                emailRequest.setBody("Вынуждены вас огорчить, ваше резюме не доставлено работодателю. Попробуйте снова.");
                sendEmail(emailRequest, accessToken, resumeFile);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/convenient_job_search/readyResume/sent?vacancyId=" + vacancyId))
                        .build();
            }
            // Перенаправление на страницу с успешным сообщением
            session.setAttribute("message", "Ваше резюме успешно отправлено!");
            session.setAttribute("submitPageUrl", submitPageUrl); // Сохранение submitPageUrl в сессии
            emailRequest.setTo(email);
            emailRequest.setSubject("Отправка резюме");
            emailRequest.setBody("Поздравляем, ваше резюме успешно отправлено в компанию " + vacancy.getCompanyName() + "! Успешного отклика и дальнейшего поднятия бабла!");
            sendEmail(emailRequest, accessToken, resumeFile);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/convenient_job_search/readyResume/sent?vacancyId=" + vacancyId))
                    .build();
        } catch (Exception e) {
            //return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
            System.out.println("Error sending resume: " + e.getMessage());
            session.setAttribute("message", "Ошибка отправки резюме: " + e.getMessage());
            session.setAttribute("submitPageUrl", submitPageUrl);
            emailRequest.setTo(email);
            emailRequest.setSubject("Отправка резюме");
            emailRequest.setBody("Вынуждены вас огорчить, ваше резюме не доставлено работодателю. Причина ошибки отправки резюме: " + e.getMessage());
            sendEmail(emailRequest, accessToken, resumeFile);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/convenient_job_search/readyResume/sent?vacancyId=" + vacancyId))
                    .build();
        }
    }

    private String extractGoogleAccessToken(Authentication authentication) {
        String accessToken = null;
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
            accessToken = client.getAccessToken().getTokenValue();
        }
        return accessToken;
    }

    public String extractEmail(Authentication authentication) {
        String email = null;
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User user = oauthToken.getPrincipal();
            email = user.getAttribute("email");
        }
        return email;
    }

    public String extractFirstName(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User user = oauthToken.getPrincipal();
            return user.getAttribute("given_name");
        }
        return null;
    }

    public String extractLastName(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User user = oauthToken.getPrincipal();
            return user.getAttribute("family_name");
        }
        return null;
    }

    private String pageForSendingResume(Vacancy vacancy) {
        String targetUrl = null;
        String targetUrlWorkUaFormat = "https://www.work.ua/ru/jobseeker/my/resumes/send/?id=%s";
        //String targetUrlRabotaUaFormat = "https://robota.ua/ru/company%s/vacancy%s/apply";
        String targetUrlRabotaUaFormat = "https://robota.ua/ru/company%s/vacancy%s/apply?newApply=true";
        String site = vacancy.getSiteName();
        String url = vacancy.getUrl();

        if (site.contains("work.ua")) {
            String regex = "jobs/(\\d+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                targetUrl = String.format(targetUrlWorkUaFormat, matcher.group(1)); // Возвращаем первую группу (число)
            }
        } else if (site.contains("robota.ua")) {
            String regex = "company(\\d+)/vacancy(\\d+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                targetUrl = String.format(targetUrlRabotaUaFormat, matcher.group(1), matcher.group(2));
            }
        }

        return targetUrl;
    }

    private static String extractIdVacancyWorkUa(String targetUrl) {
        String regex = "jobs/(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(targetUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractIdVacancyRabotaUa(String targetUrl) {
        String regex = "vacancy(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(targetUrl);
        if (matcher.find()) {
           return matcher.group(1);
        }
        return "";
    }

    public String sendEmail(EmailRequest emailRequest, String accessToken, String fileNameResume) {
        emailService.sendSimpleMessage(emailRequest.getTo(), emailRequest.getSubject(),
                emailRequest.getBody(), accessToken, fileNameResume);
        return "Email sent successfully!";
    }

    // Вспомогательный класс для передачи данных в прокси
    private static class ProxyRequest {
        private String token;
        private String filePath;
        private Long vacancyIdRabotaUa;
        private String email;
        private String firstName;
        private String lastName;
        private String resumeContent;
        private String targetLoadSendUrl;
        private String submitPageUrl;

        public ProxyRequest(String token, String filePath, Long vacancyIdRabotaUa, String email,
                            String firstName, String lastName, String resumeContent,
                            String targetLoadSendUrl, String submitPageUrl) {
            this.token = token;
            this.filePath = filePath;
            this.vacancyIdRabotaUa = vacancyIdRabotaUa;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.resumeContent = resumeContent;
            this.targetLoadSendUrl = targetLoadSendUrl;
            this.submitPageUrl = submitPageUrl;
        }

        // Геттеры
        public String getToken() {
            return token;
        }

        public String getFilePath() {
            return filePath;
        }

        public Long getVacancyIdRabotaUa() {
            return vacancyIdRabotaUa;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getResumeContent() {
            return resumeContent;
        }

        public String getTargetLoadSendUrl() {
            return targetLoadSendUrl;
        }

        public String getSubmitPageUrl() {
            return submitPageUrl;
        }
    }

    class EmailRequest {
        private String to;
        private String subject;
        private String body;

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    //для андроид
    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<String> uploadResumeAndroid(@RequestBody ResumeSendRequest request) {
        String resultMessage = "";
        String accessToken = "";
        String email = "";
        String firstName = "";
        String lastName = "";
        String submitPageUrl = "";
        EmailRequest emailRequest = new EmailRequest();
        try {
            // Получение токена, маил, firstName и lastName
            accessToken = request.getAccessToken();
            email = request.getEmail();
            firstName = request.getFirstName();
            lastName = request.getLastName();

            // Проверка, был ли токен получен
            if (accessToken == null) {
                resultMessage = "Ошибка: не удалось получить токен доступа. Вам необходимо авторизироваться.";
                // Перенаправление на страницу с успешным сообщением
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resultMessage); // Возвращаем 401 Unauthorized
            }

            System.out.println("accessToken: " + accessToken);
            System.out.println("eMail: " + email);
            System.out.println("firstName: " + firstName);
            System.out.println("lastName: " + lastName);

            // Путь к файлу резюме в папке static/resume
            System.out.println("Получен POST-запрос на загрузку резюме");
            String filePath = "src/main/resources/static/resumes/" + request.getFileName(); // Укажите имя файла
            //Path filePath = Paths.get("src/main/resources/static/resumes").resolve(resumeFile).normalize();
            //System.out.println(filePath);
            File file = new File(filePath);
            FileSystemResource resource = new FileSystemResource(file);

            Vacancy vacancy = request.getVacancy();
            System.out.println(vacancy.getId());
            System.out.println(vacancy.getTitle());
            System.out.println(vacancy.getCompanyName());
            System.out.println(vacancy.getCity());
            System.out.println(vacancy.getSiteName());
            System.out.println(vacancy.getUrl());
            submitPageUrl = pageForSendingResume(vacancy); // URL страницы где происходит отправка резюме
            System.out.println("submitPageUrl: " + submitPageUrl);

            if (vacancy.getSiteName().contains("robota.ua")) {
                System.out.println("Site is robota.ua");
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String encodedFile = Base64.getEncoder().encodeToString(fileBytes);

                String targetProxyLoadSendUrl = "https://unlimitedpossibilities12.org/api/proxy/upload-send-resume-rabota-ua";
                String vacancyIdRabotaUa = extractIdVacancyRabotaUa(vacancy.getUrl());
                long vacancyIdRabotaUaLong = Long.parseLong(vacancyIdRabotaUa);
                String targetLoadSendUrl = "https://apply-api.robota.ua/attach-application";

                // Создание объекта запроса для прокси
                ProxyRequest proxyRequest = new ProxyRequest(accessToken, filePath,
                        vacancyIdRabotaUaLong, email, firstName, lastName, encodedFile,
                        targetLoadSendUrl, submitPageUrl);

                // Отправка POST-запросов через прокси
                ResponseEntity<String> responseLoadAndSend = customRestTemplate.postForEntity(targetProxyLoadSendUrl, proxyRequest, String.class);

                // Проверка ответа
                if (responseLoadAndSend.getStatusCode() == HttpStatus.OK) {
                    System.out.println("Resume successfully sent");
                } else {
                    System.out.println("Error sending resume: " + responseLoadAndSend.getStatusCode() + " - " + responseLoadAndSend.getBody());
                    resultMessage = "Ошибка отправки резюме: " + responseLoadAndSend.getStatusCode() + " - " + responseLoadAndSend.getBody();
                    emailRequest.setTo(email);
                    emailRequest.setSubject("Отправка резюме");
                    emailRequest.setBody("Вынуждены вас огорчить, ваше резюме не доставлено работодателю. Попробуйте снова.");
                    sendEmail(emailRequest, accessToken, request.getFileName());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultMessage); // Возвращаем 500 Internal Server Error с сообщением об ошибке
                }

                resultMessage = "Ваше резюме успешно отправлено!";
                emailRequest.setTo(email);
                emailRequest.setSubject("Отправка резюме");
                emailRequest.setBody("Поздравляем, ваше резюме успешно отправлено в компанию " + vacancy.getCompanyName() + "! Успешного отклика и дальнейшего поднятия бабла!");
                sendEmail(emailRequest, accessToken, request.getFileName());
                return ResponseEntity.ok(resultMessage); // Возвращаем 200 OK и сообщение об успехе
            }

            System.out.println("Site is work.ua");

            //Без прокси, отправка на URL страницы где загрузка и отправка резюме
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("resume", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Отправка POST-запроса
            ResponseEntity<String> response = customRestTemplate.postForEntity(submitPageUrl, requestEntity, String.class);
            countRequestWorkUa++;

            // Проверка ответа
            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Resume successfully sent");
            } else {
                //из за того что самый первый запрос всегда с ошибкой 302
                if (countRequestWorkUa == 1) {
                    response = customRestTemplate.postForEntity(submitPageUrl, requestEntity, String.class);
                    if (response.getStatusCode() == HttpStatus.OK) {
                        System.out.println("Resume successfully sent");
                        resultMessage = "Ваше резюме успешно отправлено!";
                        emailRequest.setTo(email);
                        emailRequest.setSubject("Отправка резюме");
                        emailRequest.setBody("Поздравляем, ваше резюме успешно отправлено в компанию " + vacancy.getCompanyName() + "! Успешного отклика и дальнейшего поднятия бабла!");
                        sendEmail(emailRequest, accessToken, request.getFileName());
                        return ResponseEntity.ok(resultMessage); // Возвращаем 200 OK и сообщение об успехе
                    }
                }
                System.out.println("Error sending resume: " + response.getStatusCode() + " - " + response.getBody());
                resultMessage = "Ошибка отправки резюме: " + response.getStatusCode() + " - " + response.getBody();
                emailRequest.setTo(email);
                emailRequest.setSubject("Отправка резюме");
                emailRequest.setBody("Вынуждены вас огорчить, ваше резюме не доставлено работодателю. Попробуйте снова.");
                sendEmail(emailRequest, accessToken, request.getFileName());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultMessage); // Возвращаем 500 Internal Server Error
            }

            resultMessage = "Ваше резюме успешно отправлено!";
            emailRequest.setTo(email);
            emailRequest.setSubject("Отправка резюме");
            emailRequest.setBody("Поздравляем, ваше резюме успешно отправлено в компанию " + vacancy.getCompanyName() + "! Успешного отклика и дальнейшего поднятия бабла!");
            sendEmail(emailRequest, accessToken, request.getFileName());
            return ResponseEntity.ok(resultMessage); // Возвращаем 200 OK и сообщение об успехе
        } catch (Exception e) {
            //return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка: " + e.getMessage());
            System.out.println("Error sending resume: " + e.getMessage());
            resultMessage = "Ошибка отправки резюме: " + e.getMessage();
            emailRequest.setTo(email);
            emailRequest.setSubject("Отправка резюме");
            emailRequest.setBody("Вынуждены вас огорчить, ваше резюме не доставлено работодателю. Причина ошибки отправки резюме: " + e.getMessage());
            sendEmail(emailRequest, accessToken, request.getFileName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultMessage); // Возвращаем 500 Internal Server Error
        }
    }
}
