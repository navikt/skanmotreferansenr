package no.nav.skanmotreferansenr.metrics;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitor {

	String value() default "";

	String[] extraTags() default {};

	double[] percentiles() default {};

	boolean histogram() default true;

	String description() default "";

	boolean logException() default true;
}
