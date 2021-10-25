package service;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

/**
 * The list of partners.
 *
 * @author vsabbisetty
 */
@Getter
@Builder
@Jacksonized
public class Partners {
  /**
   * The list of partners.
   */
  @Singular
  private final List<Partner> partners;
}
