package com.junksail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class TicketAnalyzer {

    //Константы для форматирования даты и времени
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    public static void main(String[] args) throws IOException {
        File file = new File("src/main/resources/tickets.json");

        // Чтение из JSON-файла
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // добавлен для корректной работы с классами LocalTime/Date
        JsonNode rootNode = mapper.readTree(file);
        JsonNode ticketsNode = rootNode.path("tickets");

        // Создание списка билетов
        List<Ticket> tickets = new ArrayList<>();
        for (JsonNode ticketNode : ticketsNode) {
            Ticket ticket = mapper.treeToValue(ticketNode, Ticket.class);
            tickets.add(ticket);
        }

        // Минимальное время полета
        Map<String, Integer> minFlightTimesByCarrier = findMinFlightTimes(tickets);
        System.out.println("Минимальное время полета между Владивостоком и Тель-Авивом для каждого перевозчика:");
        getFlightTimes(minFlightTimesByCarrier);

        // Разница между средней ценой и медианой
        List<Double> prices = getPricesForRoute(tickets);
        double averagePrice = calculateAverage(prices);
        double medianPrice = calculateMedian(prices);
        System.out.println("Разница между средней ценой и медианой для полета между Владивостоком и Тель-Авивом:");
        System.out.println("Средняя цена: " + averagePrice);
        System.out.println("Медиана: " + medianPrice);
        System.out.println("Разница: " + Math.abs(averagePrice - medianPrice));
    }

    // Метод возвращает Map, в котором ключ - перевозчик, а значение - минимальное время
    private static Map<String, Integer> findMinFlightTimes(List<Ticket> tickets) {
        Map<String, Integer> minFlightTimesByCarrier = new HashMap<>();
        for (Ticket ticket : tickets) {
            if (ticket.getOriginName().equals("Владивосток") && ticket.getDestinationName().equals("Тель-Авив")) {
                int flightTime = calculateFlightTime(ticket);
                minFlightTimesByCarrier.putIfAbsent(ticket.getCarrier(), flightTime);
                minFlightTimesByCarrier.computeIfPresent(ticket.getCarrier(), (carrier, time) -> Math.min(time, flightTime));
            }
        }
        return minFlightTimesByCarrier;
    }

    // Метод для пары перевозчик - минимальное время полёта
    private static void getFlightTimes(Map<String, Integer> minFlightTimesByCarrier) {
        for (Map.Entry<String, Integer> entry : minFlightTimesByCarrier.entrySet()) {
            System.out.println(entry.getKey() + ": " + formatTime(entry.getValue()));
        }
    }

    // Получаем массив цен на билеты, для нахождения средней стоимости
    private static List<Double> getPricesForRoute(List<Ticket> tickets) {
        List<Double> prices = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket.getOriginName().equals("Владивосток") && ticket.getDestinationName().equals("Тель-Авив")) {
                prices.add(ticket.getPrice());
            }
        }
        return prices;
    }

    private static int calculateFlightTime(Ticket ticket) {
        // Форматирование
        LocalDate departureDate = LocalDate.parse(ticket.getDepartureDate(), DATE_FORMATTER);
        LocalDate arrivalDate = LocalDate.parse(ticket.getArrivalDate(), DATE_FORMATTER);
        LocalTime departureTime = LocalTime.parse(ticket.getDepartureTime(), TIME_FORMATTER);
        LocalTime arrivalTime = LocalTime.parse(ticket.getArrivalTime(), TIME_FORMATTER);

        // Возвращаем длину промежутка времени полёта
        return (int) ChronoUnit.MINUTES.between(departureDate.atTime(departureTime), arrivalDate.atTime(arrivalTime));
    }

    // Форматирование времени при помощи регулярного выражения
    private static String formatTime(int minutes) {
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return String.format("%02d:%02d", hours, remainingMinutes);
    }

    // Вычисление медианы
    private static double calculateMedian(List<Double> list) {
        Collections.sort(list); // Сортировка по возрастанию
        int middle = list.size() / 2; // Находим центр массива
        if (list.size() % 2 == 1) {
            return list.get(middle); // В случае, если число элиментов нечётное, получаем результат
        } else {
            return (list.get(middle - 1) + list.get(middle)) / 2.0; // Если количество элементов чётное,
            // находим среднее значение двух центральных элементов
        }
    }

    // Расчёт средней стоимости билеты
    private static double calculateAverage(List<Double> list) {
        return list.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0); // Использую Stream для быстрого преобразования в double и вычисления average
    }
}
