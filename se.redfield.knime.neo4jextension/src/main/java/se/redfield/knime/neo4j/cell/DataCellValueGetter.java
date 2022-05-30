package se.redfield.knime.neo4j.cell;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.time.duration.DurationCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.period.PeriodCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;

public class DataCellValueGetter {

     public static Object getValue(DataCell cell) {
        if (cell instanceof ListCell) {
            return getValue((ListCell) cell);
        }
        if (cell instanceof SetCell) {
            return getValue((SetCell) cell);
        }
        if (cell instanceof BooleanCell) {
            return getValue((BooleanCell) cell);
        }
        if (cell instanceof IntCell) {
            return getValue((IntCell) cell);
        }
        if (cell instanceof LongCell) {
            return getValue((LongCell) cell);
        }
        if (cell instanceof DoubleCell) {
            return getValue((DoubleCell) cell);
        }
        if (cell instanceof LocalDateCell) {
            return getValue((LocalDateCell) cell);
        }
        if (cell instanceof LocalDateTimeCell) {
            return getValue((LocalDateTimeCell) cell);
        }
        if (cell instanceof LocalTimeCell) {
            return getValue((LocalTimeCell) cell);
        }
        if (cell instanceof ZonedDateTimeCell) {
            return getValue((ZonedDateTimeCell) cell);
        }
        if (cell instanceof DurationCell) {
            return getValue((DurationCell) cell);
        }
        if (cell instanceof PeriodCell) {
            return getValue((PeriodCell) cell);
        }
        if (cell instanceof StringValue) {
            return getValue((StringValue) cell);
        }

        return cell.toString();
    }

    public static List getValue(ListCell cell) {
        return cell.stream()
                .map(dataCell -> {
                    if (dataCell instanceof ListCell){
                        return getValue((ListCell) dataCell);
                    } else {
                        return getValue(dataCell);
                    }
                })
                .collect(Collectors.toList());
    }

    public static Set getValue(SetCell cell) {
        return cell.stream()
                .map(dataCell -> {
                    if (dataCell instanceof SetCell){
                        return getValue((SetCell) dataCell);
                    } else {
                        return getValue(dataCell);
                    }
                })
                .collect(Collectors.toSet());
    }

    public static boolean getValue(BooleanCell cell) {
         return cell.getBooleanValue();
    }

    public static int getValue(IntCell cell) {
         return cell.getIntValue();
    }

    public static long getValue(LongCell cell) {
         return cell.getLongValue();
    }

    public static double getValue(DoubleCell cell) {
         return cell.getDoubleValue();
    }

    public static LocalDate getValue(LocalDateCell cell) {
         return cell.getLocalDate();
    }

    public static LocalDateTime getValue(LocalDateTimeCell cell) {
         return cell.getLocalDateTime();
    }

    public static LocalTime getValue(LocalTimeCell cell) {
         return cell.getLocalTime();
    }

    public static ZonedDateTime getValue(ZonedDateTimeCell cell) {
         return cell.getZonedDateTime();
    }

    public static Duration getValue(DurationCell cell) {
         return cell.getDuration();
    }

    public static Period getValue(PeriodCell cell) {
         return cell.getPeriod();
    }

    public static String getValue(StringValue cell) {
         return cell.getStringValue();
    }
}
