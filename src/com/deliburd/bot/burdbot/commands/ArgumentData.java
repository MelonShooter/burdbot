package com.deliburd.bot.burdbot.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ArgumentData {
	private final Map<Class<? extends Annotation>, List<Annotation>> argumentAnnotationDataMap;
	private final String argumentDataString;
	
	private ArgumentData(String argumentDataString) {
		argumentAnnotationDataMap = Map.of();
		this.argumentDataString = argumentDataString;
	}
	
	private ArgumentData(Annotation[] annotations, String argumentDataString) {
		argumentAnnotationDataMap = new ConcurrentHashMap<>(annotations.length);
		this.argumentDataString = argumentDataString;
		
		for(Annotation annotation : annotations) {
			argumentAnnotationDataMap.compute(annotation.getClass(), (annotationClass, annotationList) -> {
				if(annotationList == null) {
					annotationList = new ArrayList<Annotation>();
				}
				
				annotationList.add(annotation);
				
				return annotationList;
			});
		}
	}
	
	/**
	 * Gets an ArgumentData object containing a command argument's string value along with the annotation data.
	 * 
	 * @param commandArgument The command argument as a parameter
	 * @param argumentDataString The argument's string value
	 * @return An ArgumentData object containing a command argument's string value along with the annotation data.
	 */
	public static ArgumentData of(Parameter commandArgument, String argumentDataString) {
		Annotation[] annotations = commandArgument.getAnnotations();
		
		return of(annotations, argumentDataString);
	}
	
	/**
	 * Gets an ArgumentData object containing a command argument's string value along with the annotation data.
	 * 
	 * @param annotations The annotation data
	 * @param argumentDataString The argument's string value
	 * @return An ArgumentData object containing a command argument's string value along with the annotation data.
	 */
	public static ArgumentData of(Annotation[] annotations, String argumentDataString) {
		if(annotations.length == 0) {
			return new ArgumentData(argumentDataString);
		}
		
		return new ArgumentData(annotations, argumentDataString);
	}
	
	/**
	 * Gets the first annotation found for the argument for the given annotation class found.
	 * 
	 * @param <T> The type of the annotation
	 * @param annotationClass The class of the annotation
	 * @return An optional annotation. If the annotation isn't found, a blank optional is returned.
	 * If there are multiple of the same annotation type found, the first one declared will be returned.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> Optional<T> getArgumentAnnotationData(Class<T> annotationClass) {
		var retrievedArgumentData = argumentAnnotationDataMap.get(annotationClass);
		Annotation annotation = null;
		
		if(retrievedArgumentData != null) {
			annotation = retrievedArgumentData.get(0);
		}
		
		return Optional.ofNullable((T) annotation);
	}
	
	/**
	 * Gets all of an argument's annotation data for a certain annotation class.
	 * 
	 * @param <T> The type of the annotation
	 * @param annotationClass The class of the annotation
	 * @return An unmodifiable list of the argument's annotation data for a certain annotation class. Returns an empty list if no data is found. 
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> getAllArgumentAnnotationData(Class<T> annotationClass) {
		List<Annotation> retrievedAnnotationList = argumentAnnotationDataMap.get(annotationClass); 
		
		if(retrievedAnnotationList == null) {
			return List.of();
		} else if(retrievedAnnotationList.size() == 1) {
			return Collections.singletonList((T) retrievedAnnotationList.get(0));
		}
		
		List<T> argumentData = retrievedAnnotationList.stream()
				.map(annotation -> (T) annotation)
				.collect(Collectors.toUnmodifiableList());
				
		return argumentData;
	}

	/**
	 * Gets the argument's data string.
	 * 
	 * @return The argument's data string.
	 */
	public String getArgumentDataString() {
		return argumentDataString;
	}
}
