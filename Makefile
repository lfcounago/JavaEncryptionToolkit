# Comando para compilar GenerarClaves.java
compile_generar_claves:
    javac -cp ".;bcprov-jdk18on-176.jar" GenerarClaves.java

# Comandos para ejecutar GenerarClaves con diferentes argumentos
run_generar_claves_alumno: compile_generar_claves
    java -cp ".;bcprov-jdk18on-176.jar" GenerarClaves alumno

run_generar_claves_profesor: compile_generar_claves
    java -cp ".;bcprov-jdk18on-176.jar" GenerarClaves profesor

run_generar_claves_autoridad: compile_generar_claves
    java -cp ".;bcprov-jdk18on-176.jar" GenerarClaves autoridadSellado

# Comando para compilar EmpaquetarExamen.java
compile_empaquetar_examen:
    javac -cp ".;bcprov-jdk18on-176.jar" EmpaquetarExamen.java

# Comando para ejecutar EmpaquetarExamen
run_empaquetar_examen: compile_empaquetar_examen
    java -cp ".;bcprov-jdk18on-176.jar" EmpaquetarExamen examen paquete .\\profesor.publica .\\alumno.privada

# Comando para compilar SellarExamen.java
compile_sellar_examen:
    javac -cp ".;bcprov-jdk18on-176.jar" SellarExamen.java

# Comando para ejecutar SellarExamen
run_sellar_examen: compile_sellar_examen
    java -cp ".;bcprov-jdk18on-176.jar" SellarExamen paquete .\\alumno.publica .\\autoridadSellado.privada

# Comando para compilar DesempaquetarExamen.java
compile_desempaquetar_examen:
    javac -cp ".;bcprov-jdk18on-176.jar" DesempaquetarExamen.java

# Comando para ejecutar DesempaquetarExamen
run_desempaquetar_examen: compile_desempaquetar_examen
    java -cp ".;bcprov-jdk18on-176.jar" DesempaquetarExamen paquete .\\autoridadSellado.publica .\\profesor.privada

# Comando para limpiar los archivos .class generados
clean:
    rm -f *.class

# Comando para compilar y ejecutar todo el proceso
all: run_generar_claves_alumno run_generar_claves_profesor run_generar_claves_autoridad run_empaquetar_examen run_sellar_examen run_desempaquetar_examen clean
