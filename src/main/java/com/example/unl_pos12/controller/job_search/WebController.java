package com.example.unl_pos12.controller.job_search;
import com.example.unl_pos12.model.job_search.Provider;
import com.example.unl_pos12.model.job_search.RabotaUaStrategy;
import com.example.unl_pos12.model.job_search.Vacancy;
import com.example.unl_pos12.model.job_search.WorkUaStrategy;
import com.example.unl_pos12.parameters.City;
import com.example.unl_pos12.parameters.Language;
import com.example.unl_pos12.parameters.TimeDate;
import com.example.unl_pos12.repo.VacancyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class WebController {
    private static List<Vacancy> fullVacancies;
    private static List<Vacancy> fromWorkUaVacancies;
    private static List<Vacancy> fromRabotaUaVacancies;
    @Autowired
    private VacancyRepository vacancyRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/convenient_job_search")
    public String mainPage(Model model) {
        model.addAttribute("title", "Поиск ");
        model.addAttribute("title2", "работы");
        return "job_search/siteVacancies";
    }

    @PostMapping("/convenient_job_search/search_result")
    public String handleSearch(@RequestParam(name = "work-ua", required = false) boolean workUa,
                               @RequestParam(name = "rabota-ua", required = false) boolean rabotaUa,
                               @RequestParam(name = "max-vacancies-work", required = false) Integer maxVacanciesWorkUa,
                               @RequestParam(name = "max-vacancies-rabota", required = false) Integer maxVacanciesRabotaUa,
                               @RequestParam(name = "inputPosition", required = false) String inputPosition,
                               @RequestParam(name = "city", required = false) String city,
                               @RequestParam(name = "language", required = false) String language,
                               @RequestParam(name = "timeframe", required = false) String timeframe,
                               Model model, HttpServletRequest request) {

        HttpSession session = request.getSession();
        String sessionId = session.getId(); // Получаем идентификатор сессии
        System.out.println("sessionId: " + sessionId);

        List<Provider> providersList = new ArrayList<>();
        String result = "";

        if (workUa && rabotaUa) {
            // Обработка выбора Work.ua и Rabota.ua
            Provider providerWorkUa = new Provider(new WorkUaStrategy());
            Provider providerRabotaUa = new Provider(new RabotaUaStrategy());
            providersList.add(providerWorkUa);
            providersList.add(providerRabotaUa);
            result = "Выбран Work.ua и Rabota.ua";
            model.addAttribute("title", result);
            System.out.println(result);
        } else if (workUa) {
            // Обработка выбора Work.ua
            Provider providerWorkUa = new Provider(new WorkUaStrategy());
            providersList.add(providerWorkUa);
            result = "Выбран Work.ua";
            model.addAttribute("title", result);
            System.out.println(result);
        } else if (rabotaUa) {
            // Обработка выбора Rabota.ua
            Provider providerRabotaUa = new Provider(new RabotaUaStrategy());
            providersList.add(providerRabotaUa);
            result = "Выбран Rabota.ua";
            model.addAttribute("title", result);
            System.out.println(result);
        } else if (!workUa && !rabotaUa) {
            return "redirect:/convenient_job_search";
        }

        Provider[] providers = providersList.toArray(new Provider[providersList.size()]);
        com.example.unl_pos12.controller.job_search.Controller controller = startConfiguration(providers);
        /*System.out.println("language : "  + language);
        System.out.println("city : "  + city);
        System.out.println("time : "  + timeframe);*/

        if ((city == null || city.isEmpty()) && (language == null || language.isEmpty()) &&
                (timeframe == null || timeframe.isEmpty())) {
            controller.onPositionSelect(inputPosition, maxVacanciesWorkUa, maxVacanciesRabotaUa);
        } else {
            Language language1 = null;
            City city1 = null;
            TimeDate timeDate1 = null;
            if (city == null || city.isEmpty()) {
                city1 = City.KIEV;
            }
            if (language == null || language.isEmpty()) {
                language1 = Language.RUSSIAN;
            }
            if (timeframe == null || timeframe.isEmpty()) {
                timeDate1 = TimeDate.THIRTY_DAYS;
            }
            switch (language) {
                case "ru" -> language1 = Language.RUSSIAN;
                case "uk" -> language1 = Language.UKRAINIAN;
                case "en" -> language1 = Language.ENGLISH;
            }
            switch (city) {
                case "kiev" -> city1 = City.KIEV;
                case "kharkiv" -> city1 = City.KHARKOV;
                case "odesa" -> city1 = City.ODESSA;
                case "dnipro" -> city1 = City.DNEPROPETROVSK;
            }
            switch (timeframe) {
                case "1" -> timeDate1 = TimeDate.ONE_DAY;
                case "3" -> timeDate1 = TimeDate.THREE_DAYS;
                case "7" -> timeDate1 = TimeDate.SEVEN_DAYS;
                case "14" -> timeDate1 = TimeDate.FOURTEEN_DAYS;
                case "30" -> timeDate1 = TimeDate.THIRTY_DAYS;
            }
            controller.onParamSelect(language1, city1, inputPosition, timeDate1,
                    maxVacanciesWorkUa, maxVacanciesRabotaUa);
        }

        while (true) {
            if (workUa && rabotaUa) {
                if (fullVacancies != null) {
                    for (Vacancy vacancy : fullVacancies) {
                        vacancy.setSessionId(sessionId);
                        vacancyRepository.save(vacancy);
                    }
                    model.addAttribute("vacancies", fullVacancies);
                    break;
                }
            } else if (workUa) {
                if (fromWorkUaVacancies != null) {
                    for (Vacancy vacancy : fromWorkUaVacancies) {
                        vacancy.setSessionId(sessionId);
                        vacancyRepository.save(vacancy);
                    }
                    model.addAttribute("vacancies", fromWorkUaVacancies);
                    break;
                }
            } else if (rabotaUa) {
                if (fromRabotaUaVacancies != null) {
                    for (Vacancy vacancy : fromRabotaUaVacancies) {
                        vacancy.setSessionId(sessionId);
                        vacancyRepository.save(vacancy);
                    }
                    model.addAttribute("vacancies", fromRabotaUaVacancies);
                    break;
                }
            }
        }

        return "job_search/resultSearchVacancies";
    }

    // Новый метод для Android
    @PostMapping("/api/convenient_job_search/search_result")
    @ResponseBody
    public ResponseEntity<List<Vacancy>> handleApiSearch(@RequestParam(name = "work-ua", required = false) boolean workUa,
                               @RequestParam(name = "rabota-ua", required = false) boolean rabotaUa,
                               @RequestParam(name = "max-vacancies-work", required = false) Integer maxVacanciesWorkUa,
                               @RequestParam(name = "max-vacancies-rabota", required = false) Integer maxVacanciesRabotaUa,
                               @RequestParam(name = "inputPosition", required = false) String inputPosition,
                               @RequestParam(name = "city", required = false) String city,
                               @RequestParam(name = "language", required = false) String language,
                               @RequestParam(name = "timeframe", required = false) String timeframe) {

        List<Vacancy> vacancies = new ArrayList<>();
        List<Provider> providersList = new ArrayList<>();
        String result = "";

        if (workUa && rabotaUa) {
            // Обработка выбора Work.ua и Rabota.ua
            Provider providerWorkUa = new Provider(new WorkUaStrategy());
            Provider providerRabotaUa = new Provider(new RabotaUaStrategy());
            providersList.add(providerWorkUa);
            providersList.add(providerRabotaUa);
            result = "Выбран Work.ua и Rabota.ua";
            System.out.println(result);
        } else if (workUa) {
            // Обработка выбора Work.ua
            Provider providerWorkUa = new Provider(new WorkUaStrategy());
            providersList.add(providerWorkUa);
            result = "Выбран Work.ua";
            System.out.println(result);
        } else if (rabotaUa) {
            // Обработка выбора Rabota.ua
            Provider providerRabotaUa = new Provider(new RabotaUaStrategy());
            providersList.add(providerRabotaUa);
            result = "Выбран Rabota.ua";
            System.out.println(result);
        } else if (!workUa && !rabotaUa) {
            return ResponseEntity.ok(vacancies);
        }

        Provider[] providers = providersList.toArray(new Provider[providersList.size()]);
        com.example.unl_pos12.controller.job_search.Controller controller = startConfiguration(providers);
        /*System.out.println("language : "  + language);
        System.out.println("city : "  + city);
        System.out.println("time : "  + timeframe);*/

        if ((city == null || city.isEmpty()) && (language == null || language.isEmpty()) &&
                (timeframe == null || timeframe.isEmpty())) {
            controller.onPositionSelect(inputPosition, maxVacanciesWorkUa, maxVacanciesRabotaUa);
        } else {
            Language language1 = null;
            City city1 = null;
            TimeDate timeDate1 = null;
            if (city == null || city.isEmpty()) {
                city1 = City.KIEV;
            }
            if (language == null || language.isEmpty()) {
                language1 = Language.RUSSIAN;
            }
            if (timeframe == null || timeframe.isEmpty()) {
                timeDate1 = TimeDate.THIRTY_DAYS;
            }
            switch (language) {
                case "ru" -> language1 = Language.RUSSIAN;
                case "uk" -> language1 = Language.UKRAINIAN;
                case "en" -> language1 = Language.ENGLISH;
            }
            switch (city) {
                case "kiev" -> city1 = City.KIEV;
                case "kharkiv" -> city1 = City.KHARKOV;
                case "odesa" -> city1 = City.ODESSA;
                case "dnipro" -> city1 = City.DNEPROPETROVSK;
            }
            switch (timeframe) {
                case "1" -> timeDate1 = TimeDate.ONE_DAY;
                case "3" -> timeDate1 = TimeDate.THREE_DAYS;
                case "7" -> timeDate1 = TimeDate.SEVEN_DAYS;
                case "14" -> timeDate1 = TimeDate.FOURTEEN_DAYS;
                case "30" -> timeDate1 = TimeDate.THIRTY_DAYS;
            }
            controller.onParamSelect(language1, city1, inputPosition, timeDate1,
                    maxVacanciesWorkUa, maxVacanciesRabotaUa);
        }

        while (true) {
            if (workUa && rabotaUa) {
                if (fullVacancies != null) {
                    for (Vacancy vacancy : fullVacancies) {
                        vacancy.setSessionId("");
                        vacancyRepository.save(vacancy);
                    }
                    vacancies = fullVacancies;
                    break;
                }
            } else if (workUa) {
                if (fromWorkUaVacancies != null) {
                    for (Vacancy vacancy : fromWorkUaVacancies) {
                        vacancy.setSessionId("");
                        vacancyRepository.save(vacancy);
                    }
                    vacancies = fromWorkUaVacancies;
                    break;
                }
            } else if (rabotaUa) {
                if (fromRabotaUaVacancies != null) {
                    for (Vacancy vacancy : fromRabotaUaVacancies) {
                        vacancy.setSessionId("");
                        vacancyRepository.save(vacancy);
                    }
                    vacancies = fromRabotaUaVacancies;
                    break;
                }
            }
        }

        return ResponseEntity.ok(vacancies);
    }

    @GetMapping("/convenient_job_search/job_search_history")
    public String jobSearchHistory(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        String sessionId = session.getId(); // Получаем идентификатор сессии
        System.out.println("sessionId: " + sessionId);

        //Iterable<Vacancy> vacanciesFullListHistory = vacancyRepository.findAll();

        // Извлекаем вакансии по sessionId
        List<Vacancy> vacanciesFullListHistory = vacancyRepository.findBySessionId(sessionId);
        model.addAttribute("vacanciesFullListHistory", vacanciesFullListHistory);
        return "job_search/jobSearchHistory";
    }

    @DeleteMapping("/convenient_job_search/delete_search_history")
    @ResponseBody // Позволяет возвращать ответ без представления
    public ResponseEntity<String> deleteSearchHistory(HttpServletRequest request) {
        System.out.println("метод deleteSearchHistory сработал");
        try {
            HttpSession session = request.getSession();
            String sessionId = session.getId(); // Получаем идентификатор сессии
            System.out.println("sessionId: " + sessionId);

            // Удаляем все вакансии, соответствующие sessionId
            List<Vacancy> vacanciesToDelete = vacancyRepository.findBySessionId(sessionId);
            if (!vacanciesToDelete.isEmpty()) {
                vacancyRepository.deleteAll(vacanciesToDelete); // Удаляем вакансии
            }

            // Проверяем, что все записи были удалены
            long count = vacancyRepository.countBySessionId(sessionId);
            if (count == 0) {
                return ResponseEntity.ok("История поиска очищена для текущей сессии");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при удалении истории поиска");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при удалении истории поиска: " + e.getMessage());
        }
    }

    @DeleteMapping("/convenient_job_search/full_clear_database")
    @ResponseBody // Позволяет возвращать ответ без представления
    public ResponseEntity<String> deleteSearchHistory() {
        System.out.println("метод deleteSearchHistory сработал");
        try {
            vacancyRepository.deleteAll(); // Удаляем все записи

            // Получаем EntityManager
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            // Получаем имя таблицы
            String tableName = entityManager.getMetamodel()
                    .entity(Vacancy.class)
                    .getName(); //Vacancy
            // Сбрасываем AUTO_INCREMENT
            jdbcTemplate.execute("ALTER TABLE " + "vacancy" + " AUTO_INCREMENT = 1");
            //блок кода не сбрасывает id (оставил до разбирательства)

            // Проверяем, что все записи были удалены
            long count = vacancyRepository.count();
            if (count == 0) {
                return ResponseEntity.ok("История поиска очищена");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при удалении истории поиска");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при удалении истории поиска: " + e.getMessage());
        }
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        model.addAttribute("title", "Privacy policy");
        return "privacyPolicy";
    }

    @GetMapping("/terms_of_service")
    public String termsOfService(Model model) {
        model.addAttribute("title", "Terms of service");
        return "termsOfService";
    }

    private static com.example.unl_pos12.controller.job_search.Controller startConfiguration(Provider... providers) {
        com.example.unl_pos12.model.job_search.Model model = new com.example.unl_pos12.model.job_search.Model(providers);
        com.example.unl_pos12.controller.job_search.Controller controller = new com.example.unl_pos12.controller.job_search.Controller(model);
        return controller;
    }

    public static List<Vacancy> getFullVacancies() {
        return fullVacancies;
    }

    public static void setFullVacancies(List<Vacancy> fullVacancies) {
        WebController.fullVacancies = fullVacancies;
    }

    public static List<Vacancy> getFromWorkUaVacancies() {
        return fromWorkUaVacancies;
    }

    public static void setFromWorkUaVacancies(List<Vacancy> fromWorkUaVacancies) {
        WebController.fromWorkUaVacancies = fromWorkUaVacancies;
    }

    public static List<Vacancy> getFromRabotaUaVacancies() {
        return fromRabotaUaVacancies;
    }

    public static void setFromRabotaUaVacancies(List<Vacancy> fromRabotaUaVacancies) {
        WebController.fromRabotaUaVacancies = fromRabotaUaVacancies;
    }
}
