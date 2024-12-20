package com.example.unl_pos12.controller.telegram;

import com.example.unl_pos12.controller.job_search.Controller;
import com.example.unl_pos12.controller.job_search.WebController;
import com.example.unl_pos12.model.job_search.*;
import com.example.unl_pos12.parameters.City;
import com.example.unl_pos12.parameters.Language;
import com.example.unl_pos12.parameters.TimeDate;

import java.util.ArrayList;
import java.util.List;

public class TelegramJobSearch {

    protected static List<Vacancy> handleSearch(boolean workUa, boolean rabotaUa,
                                         Integer maxVacanciesWorkUa, Integer maxVacanciesRabotaUa,
                                         String inputPosition, String city) {
        System.out.println("Working method TelegramJobSearch.handleSearch...");
        List<Vacancy> listVacancies = new ArrayList<>();
        List<Provider> providersList = new ArrayList<>();
        String result = "";
        int id = 0;

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
        }

        Provider[] providers = providersList.toArray(new Provider[providersList.size()]);
        Controller controller = startConfiguration(providers);

        Language language1 = Language.RUSSIAN;
        City city1 = null;
        TimeDate timeDate1 = TimeDate.THIRTY_DAYS;

        city = city.toLowerCase();
        switch (city) {
            case "киев" -> city1 = City.KIEV;
            case "київ" -> city1 = City.KIEV;
            case "kiev" -> city1 = City.KIEV;
            case "kyiv" -> city1 = City.KIEV;
            case "харьков" -> city1 = City.KHARKOV;
            case "харків" -> city1 = City.KHARKOV;
            case "kharkiv" -> city1 = City.KHARKOV;
            case "одесса" -> city1 = City.ODESSA;
            case "одеса" -> city1 = City.ODESSA;
            case "odesa" -> city1 = City.ODESSA;
            case "днепр" -> city1 = City.DNEPROPETROVSK;
            case "дніпро" -> city1 = City.DNEPROPETROVSK;
            case "dnipro" -> city1 = City.DNEPROPETROVSK;
        }

        controller.onParamSelect(language1, city1, inputPosition, timeDate1,
                maxVacanciesWorkUa, maxVacanciesRabotaUa);

        while (true) {
            if (workUa && rabotaUa) {
                if (WebController.getFullVacancies() != null) {
                    for (Vacancy vacancy : WebController.getFullVacancies()) {
                        id++;
                        vacancy.setId(id);
                        listVacancies.add(vacancy);
                    }
                    break;
                }
            } else if (workUa) {
                if (WebController.getFromWorkUaVacancies() != null) {
                    for (Vacancy vacancy : WebController.getFromWorkUaVacancies()) {
                        id++;
                        vacancy.setId(id);
                        listVacancies.add(vacancy);
                    }
                    break;
                }
            } else if (rabotaUa) {
                if (WebController.getFromRabotaUaVacancies() != null) {
                    for (Vacancy vacancy : WebController.getFromRabotaUaVacancies()) {
                        id++;
                        vacancy.setId(id);
                        listVacancies.add(vacancy);
                    }
                    break;
                }
            }
        }
        // Остальная логика обработки формы
        return listVacancies;
    }

    private static Controller startConfiguration(Provider... providers) {
        Model model = new Model(providers);
        Controller controller = new Controller(model);
        return controller;
    }
}
