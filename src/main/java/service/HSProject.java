package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * The HS assessment application.
 *
 * @author vsabbisetty
 */
public class HSProject {
  /**
   * Main.
   *
   * @param args The args.
   */
  public static void main(final String[] args) {
    Dotenv dotenv = Dotenv.configure()
            .directory("src/main/java/service")
            .load();
    final String hubspotGetUrl = dotenv.get("BASE_URL") + "dataset?userKey=" + dotenv.get("API_KEY");
    final String hubspotPostUrl = dotenv.get("BASE_URL") + "result?userKey=" + dotenv.get("API_KEY");

    //Get the partners list
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    Partners responseForGetPartnersList = Partners.builder().build();
    try {
      responseForGetPartnersList = mapper.readValue(new URL(hubspotGetUrl), Partners.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    //Create a map of countries and partners belonging to the country
    final Map<String, List<Partner>> countrySegregatedMap = responseForGetPartnersList.getPartners().stream()
        .collect(Collectors.groupingBy(country -> country.getCountry()));

    final Map<String, LocalDate> countryAndStartDateMap = new HashMap<>();
    final List<Map<LocalDate, Integer>> intermediateDateCountList = new ArrayList<>();

    for (final Map.Entry<String, List<Partner>> countryAndPartnerList : countrySegregatedMap.entrySet()) {
      final String country = countryAndPartnerList.getKey();
      final List<Partner> partnersOfCountry = countryAndPartnerList.getValue();
      final List<Partner> partnersOfCountryWithFilteredAvailableDates = partnersOfCountry.stream().map(partner -> {
        final List<LocalDate> initialAvailableDates = partner.getAvailableDates();
        final Partner.PartnerBuilder pb = partner.toBuilder();
        //For every partner filter consecutive dates
        final List<LocalDate> filteredAvailableDates = initialAvailableDates.stream()
            .filter(inputDate -> initialAvailableDates.contains(inputDate.plusDays(1)))
            .collect(Collectors.toList());
        if (!filteredAvailableDates.isEmpty()) {
          pb.availableDates(filteredAvailableDates);
          //Convert the filtered dates into map of date and count
          intermediateDateCountList.add(setIntermediateDateMap(filteredAvailableDates));
        }
        return pb.build();
      }).collect(Collectors.toList());
      countrySegregatedMap.put(country, partnersOfCountryWithFilteredAvailableDates);
      if (!intermediateDateCountList.isEmpty()) {
        //Create a map with dates and corressponding counts for a country
        final Map<LocalDate, Integer> intermediateDateCountMap = reduceLong(intermediateDateCountList);
        //Get the nearest date with maximum count
        final LocalDate nearestDate = getRecentDate(intermediateDateCountMap);
        //Put in map the country and its corresponding start date
        countryAndStartDateMap.put(country, nearestDate);
        //Clear the list for the next iteration of the country
        intermediateDateCountList.clear();
      } else {
        //If no consecutive dates found assign null to the country
        countryAndStartDateMap.put(country, null);
      }
    }

    final List<Country> countryList = new ArrayList<>();

    final Countries.CountriesBuilder countriesAndAssociatedPartners = Countries.builder();

    for (final Map.Entry<String, List<Partner>> map : countrySegregatedMap.entrySet()) {
      final List<Partner> partners = map.getValue();

      //For every country get the partners that have the startDate in their available dates
      final List<Partner> filteredList = partners.stream().filter(p -> p.getAvailableDates()
          .contains(countryAndStartDateMap.get(p.getCountry()))).collect(Collectors.toList());

      //For every country update the partners list with only partners with startDate present
      countrySegregatedMap.put(map.getKey(), filteredList);

      final List<String> attendeesEmail = new ArrayList<>();
      filteredList.forEach(partner -> attendeesEmail.add(partner.getEmail()));

      //Create object for each country
      final Country country = Country.builder()
          .name(map.getKey())
          .attendeeCount(filteredList.size())
          .attendees(attendeesEmail)
          .startDate(countryAndStartDateMap.get(map.getKey()) != null ? countryAndStartDateMap.get(map.getKey()).toString() : null)
          .build();

      if (!countryList.contains(country)) {
        countryList.add(country);
      }

      Collections.sort(attendeesEmail);
    }
    countriesAndAssociatedPartners.countries(countryList);

    try {
      String resultJson = mapper.writeValueAsString(countriesAndAssociatedPartners.build());
      //Post the countries and associated date required for emailing
      HttpPost httpPost = new HttpPost(hubspotPostUrl);

      StringEntity entity = new StringEntity(resultJson);
      httpPost.setEntity(entity);
      httpPost.setHeader("Accept", "application/json");
      httpPost.setHeader("Content-type", "application/json");

      CloseableHttpClient client = HttpClients.createDefault();
      CloseableHttpResponse response = client.execute(httpPost);
      System.out.println(response);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Set intermediate date map.
   *
   * @param listOfAvailableDates list of available dates.
   * @return list of available dates.
   */
  public static Map<LocalDate, Integer> setIntermediateDateMap(final List<LocalDate> listOfAvailableDates) {
    return listOfAvailableDates.stream()
        .collect(Collectors.toMap(everyDate -> everyDate, everyDate -> 1));
  }

  /**
   * Reduce long values..
   *
   * @param maps map of local date to integer.
   * @return map of local date to integer.
   */
  private static Map<LocalDate, Integer> reduceLong(final List<Map<LocalDate, Integer>> maps) {
    return maps.stream()
        .flatMap(map -> map.entrySet().stream())
        .reduce(new HashMap<>(), (map, e) -> {
          map.compute(e.getKey(), (key, value) -> value == null ? e.getValue() : e.getValue() + value);
          return map;
        }, (m1, m2) -> {
          throw new UnsupportedOperationException();
        });
  }

  /**
   * Get nearest date.
   *
   * @param input map of local date to integer.
   * @return localdate.
   */
  public static LocalDate getRecentDate(final Map<LocalDate, Integer> input) {
    final Set<LocalDate> setDates = new HashSet<>();
    Integer maximumValue = null;
    for (final Map.Entry<LocalDate, Integer> map: input.entrySet()) {
      if (maximumValue == null || map.getValue() > maximumValue) {
        maximumValue = map.getValue();
      }
    }

    for (final Map.Entry<LocalDate, Integer> map: input.entrySet()) {
      if (map.getValue().equals(maximumValue)) {
        setDates.add(map.getKey());
      }
    }
    return Collections.min(setDates);
  }
}
