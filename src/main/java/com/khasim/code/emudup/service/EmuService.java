package com.khasim.code.emudup.service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.khasim.code.emudup.model.BluField;
import com.khasim.code.emudup.model.BluForm;
import com.khasim.code.emudup.model.FieldType;
import com.khasim.code.emudup.model.OperationType;
import com.khasim.code.emudup.model.User;
import com.khasim.code.emudup.repository.SettingsRepository;
import com.khasim.code.emudup.repository.SettingsRepository.EmulatorForm;
import com.khasim.code.emudup.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmuService {

	private final SettingsRepository settingsRepository;
	private final UserRepository userRepository;
	
	public List<BluForm> getAllForms(String productName) {
		return settingsRepository.getAllForms(productName);
	}

	public User getUserInfo(String productName, String userName) {
		return userRepository.getUser(productName, userName);
	}

	public List<BluForm> getForms(List<String> forms, String productName) {
		return settingsRepository.getForms(forms, productName);
	}
	
	public List<Map<String, Object>> getData(String productName, String form, Integer limitBy, Set<String> filterBy, Set<String> groupBy, Set<String> orderBy, String operation) {
		log.info("getData method started execution");
		EmulatorForm emuForm = settingsRepository.getOneForm(form, productName);
		List<Map<String, Object>> convertedData = convertOriginalRows(emuForm.getForm(), emuForm.getData());
		Stream<Map<String, Object>> finalResult = convertedData.stream();;
		if(filterBy != null && filterBy.size() > 0) {
			finalResult = finalResult.filter(map -> {
				return filterBy.stream().allMatch(filter -> {
					String[] filterArr = filter.split(":");
					String filterColumn = filterArr[0]; String filterValue = filterArr[1];
					return map.get(filterColumn).toString().equals(filterValue.toString());
				});
			});
		}
		if(groupBy != null || operation != null) {
			Collector<Object, ?, Long> collector = Collectors.counting();
			String operationColumn = null, operationType = null; 
			if(operation != null) {
				String[] operationArr = operation.split(":");
				operationColumn = operationArr[0];	operationType = operationArr[1];
				collector = createCollector(OperationType.valueOf(operationType), operationColumn);
			}
			if(groupBy != null) {
				Map<Map<String, Object>, Long> groupByResult = finalResult
						.collect(Collectors.groupingBy(map ->createMultiGroupKey(map, groupBy), collector));
				final String opCol = operationColumn, opType = operationType;
				finalResult = groupByResult.entrySet().stream().map(entry -> addOperationValueToMap(entry.getKey(), entry.getValue(), opCol, opType));
			}else {
				Long operationResult = ((Number) finalResult.collect(collector)).longValue();
				Map<String, Object> opFinalResult = addOperationValueToMap(null, operationResult, operationColumn, operationType);
				return Arrays.asList(opFinalResult);
			}
		}
		if(orderBy != null && orderBy.size() > 0) {
			Iterator<String> itr = orderBy.iterator();
			Comparator<Map<String, Object>> comparator = createChainComparator(itr.next(), emuForm.getForm());
			while(itr.hasNext()) {
				comparator = comparator.thenComparing(createChainComparator(itr.next(), emuForm.getForm()));
			}
			finalResult = finalResult.sorted(comparator);
		}
		if(limitBy != null) {
			finalResult = finalResult.limit(limitBy);
		}
		return finalResult.collect(Collectors.toList());
	}
	
	public Map<String, Object> createMultiGroupKey(Map<String, Object> obj, Set<String> groupBy){
		Map<String, Object> map = new HashMap<>();
		groupBy.stream().forEach(str -> map.put(str, obj.get(str)));
		return map;
	}
	
	private Map<String, Object> addOperationValueToMap(Map<String, Object> key, Object value, String column, String op) {
		if(key == null) {
			key = new HashMap<>();
		}
		if(column != null) {
			key.put(String.format("%s(%s)", op, column), value);
		}else {
			key.put("count", value);
		}
		return key;
	}
	
	private List<Map<String, Object>> convertOriginalRows(BluForm form, List<Object[]> data) {
		List<BluField> bluFields = form.getSchema();
		List<Map<String, Object>> convertedData = IntStream.range(0, data.size())
			.mapToObj(index ->{
				Map<String, Object> map = new HashMap<>();
				Object[] arr = data.get(index);
				for(int i=0;i<bluFields.size();i++) {
					map.put(bluFields.get(i).getFieldName(), arr[i]);
				}
				return map;
			})
			.collect(Collectors.toList());
		return convertedData;
	}
	
	public Comparator<Object> createComparator(FieldType type) {
		switch(type) {
			case Number :
				return Comparator.comparing(value -> (Integer) value);
			case Boolean :
				return Comparator.comparing(value -> (Boolean) value);
			case DateTime :
				return Comparator.comparing(value -> ZonedDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse((String) value)));
			default : 
				return Comparator.comparing(value -> (String) value);
		}
	}
	
	public Comparator<Map<String, Object>> createChainComparator(String orderBy, BluForm form) {
		String[] sortArr = orderBy.split(":");
		String sortColumn = sortArr[0]; String sortDir = sortArr.length > 1 ? sortArr[1] : "ASC" ;
		Comparator<Object> innerComparator = Comparator.nullsLast(createComparator(form.getField(sortColumn).getFieldType()));
		if (sortDir.equals("DESC")) {
			innerComparator = innerComparator.reversed();
		}
		Comparator<Map<String, Object>> comparator = Comparator.comparing(
				m -> m.get(sortColumn), innerComparator);
		return comparator;
	}
	
	public Collector createCollector(OperationType op, String opColumn) {
		ToIntFunction<Map<String, Object>> function = m -> (Integer) m.get(opColumn);
		Function<Map<String, Object>, Integer> function2 = m -> (Integer) m.get(opColumn);
		switch(op) {
			case Count:
				return Collectors.counting();
			case Sum:
				return Collectors.summingInt(function);
			case Avg:
				return Collectors.averagingInt(function);
			case Max:
				return Collectors.maxBy(Comparator.comparing(function2));
			case Min:
				return Collectors.minBy(Comparator.comparing(function2));
			default:
				return Collectors.counting();
		}
	}
}
