package dk.ilios.realmfieldnames

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec

import java.io.IOException

import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier

/**
 * Class responsible for creating the final output files.
 */
class FileGenerator(private val filer: Filer) {
    private val formatter: FieldNameFormatter

    init {
        this.formatter = FieldNameFormatter()
    }

    /**
     * Generates all the "&lt;class&gt;Fields" fields with field name references.
     * @param fileData Files to create.
     * *
     * @return `true` if the files where generated, `false` if not.
     */
    fun generate(fileData: Set<ClassData>): Boolean {
        return fileData
                .filter { !it.libraryClass }
                .all { generateFile(it, fileData) }
    }

    private fun generateFile(classData: ClassData, classPool: Set<ClassData>): Boolean {

        val fileBuilder = TypeSpec.classBuilder(classData.simpleClassName + "Fields")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("This class enumerate all queryable fields in {@link \$L.\$L}\n",
                        classData.packageName, classData.simpleClassName)

        // Add a static field reference to each queryable field in the Realm model class
        doGenerateFile(classData, classPool, fileBuilder, "")

        return writeToFile(classData, fileBuilder)

    }

    private fun doGenerateFile(classData: ClassData, classPool: Set<ClassData>, fileBuilder: TypeSpec.Builder, prefix: String) {

        println("Fields for class ${classData.simpleClassName}: ${classData.fields.size}")

        classData.fields.forEach { fieldName, value ->

            println("Looking at field ${fieldName} with value ${value} for class ${classData.simpleClassName}")

            if (value != null) {

                var found = false
                for (data in classPool) {
                    if (data.qualifiedClassName == value) {
                        println("Generating ${prefix}${data.simpleClassName}")
                        val linkedTypeSpec = TypeSpec.classBuilder(formatter.format(fieldName))
                                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)

                        addField(linkedTypeSpec, "$", prefix + fieldName)

                        doGenerateFile(data, classPool, linkedTypeSpec, prefix + fieldName + ".")
                        fileBuilder.addType(linkedTypeSpec.build())

                        found = true
                        break;
                    }
                }

                if(!found) {
                    addField(fileBuilder, fieldName, prefix + fieldName)
                }

            } else {
                // Add normal field name
                println("Generating field ${prefix}${fieldName} in class ${classData.simpleClassName}")
                addField(fileBuilder, fieldName, prefix + fieldName)
            }
        }
    }

    private fun writeToFile(classData: ClassData, fileBuilder: TypeSpec.Builder): Boolean {
        val javaFile = JavaFile.builder(classData.packageName, fileBuilder.build()).build()
        try {
            javaFile.writeTo(filer)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    private fun addField(fileBuilder: TypeSpec.Builder, fieldName: String, fieldNameValue: String) {
        val field = FieldSpec.builder(String::class.java, formatter.format(fieldName))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("\$S", fieldNameValue)
                .build()
        fileBuilder.addField(field)
    }
}
