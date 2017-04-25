package dk.ilios.realmfieldnames;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

/**
 * Class responsible for creating the final output files.
 */
public class FileGenerator {

	private final Filer filer;

	private final FieldNameFormatter formatter;

	public FileGenerator(Filer filer) {
		this.filer = filer;
		this.formatter = new FieldNameFormatter();
	}

	/**
	 * Generates all the "&lt;class&gt;Fields" fields with field name references.
	 *
	 * @param fileData Files to create.
	 * @return {@code true} if the files where generated, {@code false} if not.
	 */
	public boolean generate(Set<ClassData> fileData) {
		for (ClassData classData : fileData) {
			if (!generateFile(classData, fileData)) {
				return false;
			}
		}

		return true;
	}

	private boolean generateFile(ClassData classData, Set<ClassData> classPool) {

		TypeSpec.Builder fileBuilder = TypeSpec.classBuilder(classData.getSimpleClassName() + "Fields")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addJavadoc("This class enumerate all queryable fields in {@link $L.$L}\n",
						classData.getPackageName(), classData.getSimpleClassName());

		doGenerateFile(classData, classPool, fileBuilder, "", new ArrayList<String>());

		return writeToFile(classData, fileBuilder);
	}

	private void doGenerateFile(ClassData classData, Set<ClassData> classPool, TypeSpec.Builder builder, String prefix, List<String> classHierarchy) {

		classData.getFields().forEach((fieldName, value) -> {

			if (value != null) {

				classPool.stream().filter(cd -> cd.getQualifiedClassName().equals(value)).findFirst().ifPresent(data -> {

					if (classHierarchy.contains(data.getQualifiedClassName())) {
						addField(builder, fieldName, prefix + fieldName);
					} else {
						TypeSpec.Builder linkedTypeSpec = TypeSpec.classBuilder(formatter.format(fieldName))
								.addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC);
						addField(linkedTypeSpec, "$", prefix + fieldName);

						classHierarchy.add(classData.getQualifiedClassName());
						doGenerateFile(data, classPool, linkedTypeSpec, prefix + fieldName + ".", classHierarchy);
						classHierarchy.remove(classData.getQualifiedClassName());

						builder.addType(linkedTypeSpec.build());
					}

				});

			} else {
				addField(builder, fieldName, prefix + fieldName);
			}

		});

	}

	private boolean writeToFile(ClassData classData, TypeSpec.Builder fileBuilder) {
		JavaFile javaFile = JavaFile.builder(classData.getPackageName(), fileBuilder.build()).build();
		try {
			javaFile.writeTo(filer);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private void addField(TypeSpec.Builder fileBuilder, String fieldName, String fieldNameValue) {
		FieldSpec field = FieldSpec.builder(String.class, formatter.format(fieldName))
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
				.initializer("$S", fieldNameValue)
				.build();
		fileBuilder.addField(field);
	}
}
