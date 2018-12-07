package api.support.fixtures;

import api.support.builders.CalendarBuilder;
import api.support.builders.OpeningDayPeriodBuilder;
import org.folio.circulation.domain.OpeningDayPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.folio.circulation.domain.OpeningDay.createOpeningDay;
import static org.folio.circulation.domain.OpeningDayPeriod.createDayPeriod;
import static org.folio.circulation.domain.OpeningHour.createOpeningHour;
import static org.folio.circulation.domain.Weekdays.createWeekdays;
import static org.folio.circulation.resources.CheckOutByBarcodeResource.DATE_TIME_FORMATTER;

public class CalendarExamples {

  public static final String CASE_FRI_SAT_MON_DAY_ALL_SERVICE_POINT_ID = "11111111-2f09-4bc9-8924-3734882d44a3";
  public static final String CASE_FRI_SAT_MON_SERVICE_POINT_ID = "22222222-2f09-4bc9-8924-3734882d44a3";
  public static final String CASE_WED_THU_FRI_DAY_ALL_SERVICE_POINT_ID = "33333333-2f09-4bc9-8924-3734882d44a3";
  public static final String CASE_WED_THU_FRI_SERVICE_POINT_ID = "44444444-2f09-4bc9-8924-3734882d44a3";

  static final String CASE_START_DATE_MONTHS_AGO_AND_END_DATE_THU = "12345698-2f09-4bc9-8924-3734882d44a3";

  static final String CASE_START_DATE_MONTHS_AGO_AND_END_DATE_WED = "77777777-2f09-4bc9-8924-3734882d44a3";
  static final String CASE_START_DATE_FRI_AND_END_DATE_NEXT_MONTHS = "88888888-2f09-4bc9-8924-3734882d44a3";

  public static final String WEDNESDAY_DATE = "2018-12-11Z";
  public static final String THURSDAY_DATE = "2018-12-12Z";
  private static final String FRIDAY_DATE = "2018-12-13Z";

  private static final Map<String, OpeningDayPeriodBuilder> fakeOpeningPeriods = new HashMap<>();

  static {
    fakeOpeningPeriods.put(CASE_WED_THU_FRI_SERVICE_POINT_ID, new OpeningDayPeriodBuilder(CASE_WED_THU_FRI_DAY_ALL_SERVICE_POINT_ID,
      // prev day
      createDayPeriod(
        createWeekdays("WEDNESDAY"),
        createOpeningDay(Arrays.asList(createOpeningHour("08:00", "12:00"), createOpeningHour("14:00", "19:00")),
          WEDNESDAY_DATE, false, true, false)
      ),
      // current day
      createDayPeriod(
        createWeekdays("THURSDAY"),
        createOpeningDay(Arrays.asList(createOpeningHour("08:00", "12:00"), createOpeningHour("14:00", "19:00")),
          THURSDAY_DATE, false, true, false)
      ),
      // next day
      createDayPeriod(
        createWeekdays("FRIDAY"),
        createOpeningDay(Arrays.asList(createOpeningHour("08:00", "12:00"), createOpeningHour("14:00", "19:00")),
          FRIDAY_DATE, false, true, false)
      )));
    fakeOpeningPeriods.put(CASE_WED_THU_FRI_DAY_ALL_SERVICE_POINT_ID, new OpeningDayPeriodBuilder(CASE_WED_THU_FRI_DAY_ALL_SERVICE_POINT_ID,
      // prev day
      createDayPeriod(
        createWeekdays("WEDNESDAY"),
        createOpeningDay(new ArrayList<>(), WEDNESDAY_DATE, true, true, false)
      ),
      // current day
      createDayPeriod(
        createWeekdays("THURSDAY"),
        createOpeningDay(new ArrayList<>(), THURSDAY_DATE, true, true, false)
      ),
      // next day
      createDayPeriod(
        createWeekdays("FRIDAY"),
        createOpeningDay(new ArrayList<>(), FRIDAY_DATE, true, true, false)
      )));
    fakeOpeningPeriods.put(CASE_FRI_SAT_MON_DAY_ALL_SERVICE_POINT_ID, new OpeningDayPeriodBuilder(CASE_FRI_SAT_MON_DAY_ALL_SERVICE_POINT_ID,
      // prev day
      createDayPeriod(
        createWeekdays("FRIDAY"),
        createOpeningDay(new ArrayList<>(), "2018-12-14Z", true, true, false)
      ),
      // current day
      createDayPeriod(
        createWeekdays("SATURDAY"),
        createOpeningDay(new ArrayList<>(), "2018-12-15Z", false, false, false)
      ),
      // next day
      createDayPeriod(
        createWeekdays("MONDAY"),
        createOpeningDay(new ArrayList<>(), "2018-12-17Z", true, true, false)
      )));
    fakeOpeningPeriods.put(CASE_FRI_SAT_MON_SERVICE_POINT_ID, new OpeningDayPeriodBuilder(CASE_FRI_SAT_MON_SERVICE_POINT_ID,
      // prev day
      createDayPeriod(
        createWeekdays("FRIDAY"),
        createOpeningDay(Arrays.asList(createOpeningHour("08:00", "12:00"), createOpeningHour("14:00", "19:00")),
          "2018-12-14Z", false, true, false)
      ),
      // current day
      createDayPeriod(
        createWeekdays("SATURDAY"),
        createOpeningDay(new ArrayList<>(), "2018-12-15Z", false, false, false)
      ),
      // next day
      createDayPeriod(
        createWeekdays("MONDAY"),
        createOpeningDay(Arrays.asList(createOpeningHour("08:00", "12:00"), createOpeningHour("14:00", "19:00")),
          "2018-12-17Z", false, true, false)
      )));
  }

  public static CalendarBuilder getCalendarById(String serviceId) {
    switch (serviceId) {
      case CASE_FRI_SAT_MON_SERVICE_POINT_ID:
        return new CalendarBuilder(fakeOpeningPeriods.get(serviceId));
      case CASE_FRI_SAT_MON_DAY_ALL_SERVICE_POINT_ID:
        return new CalendarBuilder(fakeOpeningPeriods.get(serviceId));
      case CASE_WED_THU_FRI_DAY_ALL_SERVICE_POINT_ID:
        return new CalendarBuilder(fakeOpeningPeriods.get(serviceId));
      case CASE_WED_THU_FRI_SERVICE_POINT_ID:
        return new CalendarBuilder(fakeOpeningPeriods.get(serviceId));

      case CASE_START_DATE_MONTHS_AGO_AND_END_DATE_THU:
        LocalDate localThursdayDate = LocalDate.parse(THURSDAY_DATE, DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER));
        LocalDateTime endDate = localThursdayDate.atTime(LocalTime.MIN);
        LocalDateTime startDate = endDate.minusMonths(1);
        return new CalendarBuilder(CASE_START_DATE_MONTHS_AGO_AND_END_DATE_THU,
          startDate, endDate);

      case CASE_START_DATE_MONTHS_AGO_AND_END_DATE_WED:
        LocalDate localWednesdayDate = LocalDate.parse(WEDNESDAY_DATE, DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER));
        LocalDateTime endDateWednesday = localWednesdayDate.atTime(LocalTime.MIN);
        LocalDateTime startDateWednesday = endDateWednesday.minusMonths(1);
        return new CalendarBuilder(CASE_START_DATE_MONTHS_AGO_AND_END_DATE_THU,
          startDateWednesday, endDateWednesday);

      case CASE_START_DATE_FRI_AND_END_DATE_NEXT_MONTHS:
        LocalDate localFridayDate = LocalDate.parse(FRIDAY_DATE, DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER));
        LocalDateTime startDateFriday = localFridayDate.atTime(LocalTime.MIN);
        LocalDateTime endDateFriday = startDateFriday.plusMonths(1);
        return new CalendarBuilder(CASE_START_DATE_MONTHS_AGO_AND_END_DATE_THU,
          startDateFriday, endDateFriday);

      default:
        return new CalendarBuilder(serviceId, "Default calendar");
    }
  }

  public static List<OpeningDayPeriod> getCurrentAndNextFakeOpeningDayByServId(String serviceId) {
    OpeningDayPeriodBuilder periodBuilder = fakeOpeningPeriods.get(serviceId);
    return Arrays.asList(periodBuilder.getCurrentPeriod(), periodBuilder.getLastPeriod());
  }

  public static OpeningDayPeriod getFirstFakeOpeningDayByServId(String serviceId) {
    return fakeOpeningPeriods.get(serviceId).getFirstPeriod();
  }

  public static OpeningDayPeriod getCurrentFakeOpeningDayByServId(String serviceId) {
    return fakeOpeningPeriods.get(serviceId).getCurrentPeriod();
  }

  public static OpeningDayPeriod getLastFakeOpeningDayByServId(String serviceId) {
    return fakeOpeningPeriods.get(serviceId).getLastPeriod();
  }
}
