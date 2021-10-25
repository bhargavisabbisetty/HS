package service;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * The partner.
 *
 * @author vsabbisetty
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@ToString
public class Partner {
  /**
   * The first name.
   */
  private final String firstName;

  /**
   * The last name.
   */
  private final String lastName;

  /**
   * The email address.
   */
  private final String email;

  /**
   * The country.
   */
  private final String country;

  /**
   * The list of available dates.
   */
  @Singular
  private final List<LocalDate> availableDates;
}
