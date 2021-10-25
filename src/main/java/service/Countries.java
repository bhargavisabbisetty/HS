package service;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * The list of countries.
 *
 * @author vsabbisetty
 */
@Builder
@Getter
@EqualsAndHashCode
@ToString
@Jacksonized
public class Countries {
  /**
   * The list of countries.
   */
  @Singular
  private final List<Country> countries;
}
