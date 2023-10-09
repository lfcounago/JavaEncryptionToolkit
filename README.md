# Uso del API Java Cryptography Architecture (JPA)

Se trata de desarrollar una herramienta para el empaquetado y distribución de exámenes que sea fiable y segura y garantice las restricciones de entrega en plazo.

Para ello se contará con una Autoridad de sellado de tiempo.

Los alumnos podrán generar su Examen Empaquetado que será remitido a la "autoridad de sellado", que verificará la identidad del alumnos que hace la entrega y le vinculará el timestamp que garantiza la fecha de entrega.

Finalmente, el profesor podrá validar este Examen Empaquetado para verificar que procede del correspondiente alumno, extraer los datos aportados por el alumno y validar la autenticidad del "sello" emitido por la "autoridad de sellado".

## Simplificaciones

- Cada uno de los participantes (ALUMNO, AUTORIDAD SELLADO, PROFESOR) podrá generar sus propios pares de claves privada y pública que se almacenarán en ficheros.

- No se contemplan los mecanismos de distribución fiable de claves públicas. Se asumirá que todas las claves públicas necesarias estarán en poder del usuario que las necesite (ALUMNO, AUTORIDAD SELLADO o PROFESOR) de forma confiable.

- El ALUMNO dispondrá de un fichero con la clave pública del PROFESOR al que enviará el Examen Empaquetado.

- La AUTORIDAD SELLADO dispondrá del fichero con la clave pública del ALUMNO que procede a "sellar" su Examen Empaquetado.

- El PROFESOR contará con la clave pública de la AUTORIDAD SELLADO, así como con la clave pública del ALUMNO para el cual se vaya a realizar la validación de su Examen Empaquetado.

- Las respuestas del ALUMNO estarán almacenadas inicialmente en un fichero de texto.

- Las distintas piezas de información que aporte cada participante (ALUMNO o AUTORIDAD SELLADO) al Examen Empaquetado tendrán (antes del cifrado/firma y después del descifrado) la forma de Strings con codificación UTF8.

- El Examen Empaquetado se materializará físicamente en un fichero o "paquete" que contendrá toda la información que vayan incorporando los distintos participantes implicados: el ALUMNO que la generó y la AUTORIDAD SELLADO que da fé de la entrega del examen y del instante concreto en ésta que tuvo lugar.

- No se contempla un almacenamiento "físico" realista del Examen Empaquetado, sólo se trata de implementar los programas para generar, sellar y validar el Examen Empaquetado conforme a las especificaciones descritas en este documento.

- Al validar el Examen Empaquetado, si todas las comprobaciones de autenticidad respecto a ALUMNO y AUTORIDAD SELLADO son correctas, se mostrará al PROFESOR los datos aportados por el ALUMNO (el texto del examen) y los datos (timestamp) incorporados por la AUTORIDAD SELLADO que haya procesado dicho Examen Empaquetado. En caso contrario se indicarán las comprobaciones que no hayan sido satisfactorias.

## Requisitos

- Asegurar la confidencialidad del contenido incluido en el Examen Empaquetado por parte del ALUMNO (sólo el PROFESOR podrá tener acceso a estos contenidos).

- Garantizar que tanto el PROFESOR como cualquier otro participante tenga la posibilidad de verificar que el Examen Empaquetado fue realmente presentado por el ALUMNO correspondiente.

- Asegurar que el contenido del "paquete" con el Examen Empaquetado (datos del ALUMNO y sello de AUTORIDAD SELLADO) que se ha recibido no haya sido modificado.

- Asegurar que ni el ALUMNO ni la AUTORIDAD SELLADO podrán repudiar el contenido incluido por ellos en el Examen Empaquetado

- Asegurar que el PROFESOR no podrá realizar cambios en el contenido del Examen Empaquetado que ha recibido.

- Contar con un mecanismo mediante el cuál la AUTORIDAD pueda garantizar la fecha en que fue "recibido" el Examen Empaquetado generado por un determinado ALUMNO. Se pretende que esta vinculación entre Examen Empaquetado y "sello" pueda ser validada por el PROFESOR o por un tercero y que no pueda ser falsificada ni por el ALUMNO, ni por la AUTORIDAD SELLADO, ni por el propio PROFESOR

- La AUTORIDAD DE SELLADO debe poder verificar que el Examen Empaquetado que va a sellar procede del ALUMNO que lo presenta.

- Asegurar un coste computacional reducido en la creación, sellado y validación del Examen Empaquetado minimizando el uso de criptografía asimétrica

## Actores

- **Alumnos**:
  podrá generar su propio par de claves (pública y privada) y será responsable de generar el Examen Empaquetado a partir del fichero de texto con el examen en claro original.

- **Autoridad Sellado**:
  podrá generar su propio par de claves (pública y privada) y será responsable de sellar el Examen Empaquetado de un ALUMNO dado, habiendo verificado previamente que ese Examen Empaquetado efectivamente fue creado por dicho ALUMNO.

- **Profesor**:
  podrá generar su propio par de claves (pública y privada) y será responsable de extraer los datos aportados por el ALUMNO en el Examen Empaquetado que le haya enviado, después de haber validado la autenticidad de la autoría de dicha entrega y validar la información incluida en el Examen Empaquetado por la AUTORIDAD SELLADO.

## Folder Structure

- [`src`](/src/): carpeta de código

  - [`DesempaquetarExamen.java`](/src/DesempaquetarExamen.java): java -cp [...] DesempaquetarExamen \<nombre paquete> \<fichero examen> \<ficheros con las claves necesarias>

    - Usado por el PROFESOR
    - Se le pasa en línea de comandos el fichero con el ”paquete” que representa al Examen Empaquetado (donde se incluyen los datos aportados por el ALUMNO y los datos de la AUTORIDAD SELLADO), el nombre del fichero donde se almacenará el examen en claro y el path de los ficheros con las claves que sean necesarias para desempaquetar y verificar la información que contiene el mencionado ”paquete”.
    - Al usuario (PROFESOR) se le indicará por pantalla el resultado de las comprobaciones que se hayan realizado sobre el Examen Empaquetado y se mostrarán los datos que incluye.
      - se indicará si los datos incluidos por el ALUMNO o por la AUTORIDAD SELLADO han sufrido modificaciones o no
      - se indicará si el ”sello” de la AUTORIDAD SELLADO es válido/auténtico y, de ser así, se mostrará la fecha de sellado.
      - una vez verificado que el ALUMNO que generó el ”paquete” es quien realmente corresponde, se descifrará el examen enviado y se almacenará en el fichero indicado el texto en claro incluido por el ALUMNO en su Examen Empaquetado (opcionalmente, también se puede presentar por pantalla)

  - [`EmpaquetarExamen.java`](/src/EmpaquetarExamen.java): java -cp [...] EmpaquetarExamen \<fichero examen> \<nombre paquete> \<ficheros con las claves necesarias>

    - Usado por el ALUMNO
    - Se le pasa en línea de comandos un fichero de texto con el contenido del examen a enviar, el nombre del paquete resultante y el path de los ficheros con las claves necesarias para el empaquetado (el número y tipo exacto de los ficheros de claves dependerá de que estrategia se haya decidido seguir).
    - Genera el fichero \<nombre paquete\> (por ejemplo examen.paquete) con el resultado de ”empaquetar” los datos que conforman el Examen Empaquetado.

  - [`GenerarClaves.java`](/src/GenerarClaves.java): java -cp [...] GenerarClaves \<identificador>

    - Usado para generar los pares de claves de los participantes: ALUMNO, AUTORIDAD SELLADO y PROFESOR
    - Se le pasa como argumento de línea de comando un identificador que se usará para componer los nombre de los archivos que se generarán
    - Genera dos ficheros: ”identificador.publica” e ”identificador.privada”, conteniendo, respectivamente, las claves pública y privada de ese usuario.

  - [`Paquete.java`](/src/Paquete.java):

    - Encapsula un paquete formado por varios bloques. Cada boque tiene un nombre (de tipo String) y un contenido (de tipo byte[]).
    - La clase provee de métodos para añadir, listar y eliminar los bloques que conforman el paquete y recuperar su contenido (byte[]).
    - También provee métodos para las operaciones de lectura y escritura de paquetes empleando un formato similar al PGP ASCII armor que usa la codificación Base64 para representar datos binarios mediante caracteres imprimibles.

  - [`SellarExamen.java`](/src/SellarExamen.java): java -cp [...] SellarExamen \<nombre paquete> \<ficheros con las claves necesarias>
    - Usado por la AUTORIDAD SELLADO
    - Se le pasa en línea de comandos el fichero con el ”paquete” a sellar y el path de los ficheros con las clave/s criptográficas necesaria/s.
    - Al ”paquete” recibido como argumento le vincula (añade) los bloques que correspondan para incorporar los datos aportados por la AUTORIDAD SELLADO (fecha y hora de entrega) y para garantizar la autenticidad de los datos de ”sellado”.
    - El resultado será el mismo fichero del ”paquete” pasado como parámetro con los nuevos datos incorporados en forma de nuevos bloques.
    - En caso de comprobar que el ALUMNO que presenta el Examen Empaquetado no se corresponde con el que realmente ha creado dicho paquete, se informará por pantalla y no se generarán los bloque de sellado.

- [`lib`](/lib/): carpeta de dependencias
