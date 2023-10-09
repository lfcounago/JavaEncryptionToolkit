import java.io.*;
import java.nio.file.*;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author lfcounago
 */
public class EmpaquetarExamen {

    public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException, InvalidKeySpecException, IOException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException {

        if (args.length != 4) {
            mensajeAyuda();
            System.exit(1);
        }

        /* Cargar provider */
        Security.addProvider(new BouncyCastleProvider()); // Usa provider BC

        /* CIFRAMOS EL EXAMEN DEL ALUMNO */

        // Generamos el fichero con la clave simétrica
        System.out.println("------Generando clave simétrica-------");
        SecretKey clave = generarClaveSecreta();
        System.out.println("Clave simétrica generada correctamente");
        System.out.println("--------------------------------------\n");

        // Cifrar el examen con la clave secreta simétrica
        System.out.println("-----Cifrando el examen-----");
        byte[] examenCifrado = cifradoDES(clave, args[0]);
        System.out.println("Examen cifrado correctamente");
        System.out.println("----------------------------\n");

        /* CIFRAMOS LA CLAVE SECRETA */

        // Cifrar la clave simétrica con la clave pública del profesor con RSA
        System.out.println("--Cifrando clave simétrica--");
        byte[] claveSimetricaCifrada = cifrarClaveSimetrica(clave, args[2]);
        System.out.println(" Clave cifrada correctamente");
        System.out.println("----------------------------\n");

        /* GENERAMOS LA FIRMA */

        // Recuperar clave privada del alumno
        PrivateKey KRalumno = recuperClavePrivada(args[3]);

        // Generar firma
        System.out.println("-------Generado firma------");
        byte[] firma = generarFirma(examenCifrado, claveSimetricaCifrada, KRalumno);
        System.out.println("Hash generado correctamente");
        System.out.println("---------------------------\n");

        /* GENERAR PAQUETE */

        // Guardamos los datos obtenidos en el paquete
        System.out.println("-------Creando paquete-------");

        Paquete p = new Paquete();

        p.anadirBloque("Examen cifrado", examenCifrado);
        System.out.println("Examen cifrado añadido");
        p.anadirBloque("Clave secreta cifrada", claveSimetricaCifrada);
        System.out.println("Clave secreta cifrada añadida");
        p.anadirBloque("Firma cifrada", firma);
        System.out.println("Firma cifrada añadida");

        p.escribirPaquete(args[1]);

        System.out.println("Paquete creado correctamente");
        System.out.println("-----------------------------\n");
    }

    /*
     * Función para generar la clave simétrica con la que se va a cifrar el examen
     * Generará un fichero clave.secreta donde se guardará la clave cifrada
     * Delvolverá la clave simétrica para cifrar el examen
     */
    public static SecretKey generarClaveSecreta() throws NoSuchAlgorithmException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        /* Crear e inicializar clave DES */
        KeyGenerator generadorDES = KeyGenerator.getInstance("DES");
        generadorDES.init(56); // clave de 56 bits
        SecretKey claveSecreta = generadorDES.generateKey();

        /* Volcar clave secreta a fichero */
        // Escribirla directamente a fichero binario
        FileOutputStream out = new FileOutputStream("clave.secreta");
        out.write(claveSecreta.getEncoded());
        out.close();

        return claveSecreta;

    }

    /*
     * Función para cifrar el examen con la clave secreta
     * Devolvemos el examen cifrado
     */
    public static byte[] cifradoDES(SecretKey clave, String examen) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {

        /* Crear cifrador */
        Cipher cifrador = Cipher.getInstance("DES/ECB/PKCS5Padding");

        /* Inicializar cifrador en modo CIFRADO */
        cifrador.init(Cipher.ENCRYPT_MODE, clave);

        // Leemos todos los bytes del fichero y lo ciframos
        byte[] examenCifrado = cifrador.doFinal(Files.readAllBytes(Paths.get(examen)));

        return examenCifrado;

    }

    /*
     * Función para cifrar la clave simétrica (clave con la que se cifró el examen)
     * con la clave pública del profesor
     * Devolverá la clave cifrada
     */
    public static byte[] cifrarClaveSimetrica(SecretKey clave, String publicaProfesor)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        // SecretKeyFactory secretKeyFactoryDES = SecretKeyFactory.getInstance("DES");

        // Obtenemos la clave pública del profesor
        PublicKey KUprofesor = recuperClavePublica(publicaProfesor);

        // Crear cifrador RSA
        Cipher cifrador = Cipher.getInstance("RSA", "BC"); // Hace uso del provider BC

        // Poner cifrador en modo CIFRADO
        cifrador.init(Cipher.ENCRYPT_MODE, KUprofesor); // Cifra con la clave pública

        // Cifro con la clave pública
        byte[] claveCifrada = cifrador.doFinal(clave.getEncoded());

        return claveCifrada;

    }

    /*
     * Función para recuperar la clave pública
     * Devuelve la PublicKey correspondiente
     */
    public static PublicKey recuperClavePublica(String clavePublica)
            throws NoSuchAlgorithmException, NoSuchProviderException, IOException, InvalidKeySpecException {

        // Crear KeyFactory formato RSA
        KeyFactory keyFactoryRSA = KeyFactory.getInstance("RSA", "BC"); // Hace uso del provider BC

        // Recuperar clave publica desde datos codificados en formato X509
        X509EncodedKeySpec clavePublicaSpec = new X509EncodedKeySpec(Files.readAllBytes(Paths.get(clavePublica)));
        PublicKey KU = keyFactoryRSA.generatePublic(clavePublicaSpec);

        return KU;
    }

    /*
     * Función para generar la firma del examen cifrado y la clave simétrica cifrada
     * Devuelve la firma correspondiente
     */
    public static byte[] generarFirma(byte[] examenCifrado, byte[] claveSimetricaCifrada, PrivateKey KRalumno)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {

        Signature firmaAlumno = Signature.getInstance("MD5withRSA", "BC");
        firmaAlumno.initSign(KRalumno);
        firmaAlumno.update(examenCifrado);
        firmaAlumno.update(claveSimetricaCifrada);
        byte[] firmaAlumnoFinal = firmaAlumno.sign();

        return firmaAlumnoFinal;
    }

    /*
     * Función para recuperar la clave privada
     * Devuelve la PrivateKey correspondiente
     */
    public static PrivateKey recuperClavePrivada(String clavePrivada)
            throws NoSuchAlgorithmException, NoSuchProviderException, IOException, InvalidKeySpecException {

        KeyFactory keyFactoryRSA = KeyFactory.getInstance("RSA", "BC"); // Hace uso del provider BC

        PKCS8EncodedKeySpec clavePrivadaSpec = new PKCS8EncodedKeySpec(Files.readAllBytes(Paths.get(clavePrivada)));
        PrivateKey KR = keyFactoryRSA.generatePrivate(clavePrivadaSpec);

        return KR;
    }

    public static void mensajeAyuda() {
        System.out.println("Empaquetador de exámenes");
        System.out.println(
                "\tSintaxis: java -cp \".;bcprov-jdk18on-176.jar\" EmpaquetarExamen examen paquete .\\profesor.publica .\\alumno.privada");
        System.out.println();
    }

}
