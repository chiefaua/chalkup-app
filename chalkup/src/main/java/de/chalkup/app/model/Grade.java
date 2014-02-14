package de.chalkup.app.model;

import java.util.Arrays;
import java.util.List;

public class Grade implements Comparable<Grade> {
    final static List<String> FONT_GRADES =
            Arrays.asList("1A", "1B", "1C", "2A", "2B", "2C", "3A", "3B", "3C", "4A", "4B", "4C",
                    "5A", "5B", "5C", "6A", "6A+", "6B", "6B+", "6C", "6C+", "7A", "7A+", "7B",
                    "7B+", "7C", "7C+", "8A", "8A+", "8B", "8B+", "8C", "8C+");
    private double value;

    public Grade(double value) {
        assert (0.0 <= value && value < 1.0);
        this.value = value;
    }

    public static Grade fromFontScale(String font) {
        int i = FONT_GRADES.indexOf(font.toUpperCase());
        assert (i != -1);
        double segment = 1.0 / FONT_GRADES.size();

        return new Grade(i * segment + (segment / 2));
    }

    public static boolean isFrontScaleGrade(String font) {
        return FONT_GRADES.contains(font.toUpperCase());
    }

    public static Grade between(Grade g1, Grade g2) {
        assert (g1.compareTo(g2) <= 0);
        return g1.plus(g2.minus(g1) / 2.0);
    }

    /**
     * Can be used for null values. Lower than regular lowest()
     */
    public static Grade zero() {
        return new Grade(0.0);
    }

    public static Grade lowest() {
        return fromFontScale(FONT_GRADES.get(0));
    }

    public static Grade highest() {
        return fromFontScale(FONT_GRADES.get(FONT_GRADES.size() - 1));
    }

    public static double oneFontGradeDifference() {
        return Grade.fromFontScale("7a+").value - Grade.fromFontScale("7a").value;
    }

    @Override
    public int compareTo(Grade o) {
        return (int) Math.signum(this.value - o.value);
    }

    public String toFontScale() {
        double segment = 1.0 / FONT_GRADES.size();
        return FONT_GRADES.get((int) Math.floor(value / segment));
    }

    public Grade plus(double value) {
        return new Grade(this.value + value);
    }

    public double minus(Grade g) {
        return this.value - g.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Grade)) return false;

        Grade grade = (Grade) o;

        return compareTo(grade) == 0;
    }

    @Override
    public int hashCode() {
        long temp = value != +0.0d ? Double.doubleToLongBits(value) : 0L;
        return (int) (temp ^ (temp >>> 32));
    }

    @Override
    public String toString() {
        return value + " (" + toFontScale() + ")";
    }
}