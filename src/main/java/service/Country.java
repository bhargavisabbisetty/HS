package service;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * The country.
 *
 * @author vsabbisetty
 */
@Builder
@Getter
@EqualsAndHashCode
@ToString
@Jacksonized
public class Country {
  /**
   * The attendee count.
   */
  private final int attendeeCount;

  /**
   * The list of attendees.
   */
  private final List<String> attendees;

  /**
   * The country name.
   */
  private final String name;

  /**
   * The start date of the event.
   */
  private final String startDate;
}
