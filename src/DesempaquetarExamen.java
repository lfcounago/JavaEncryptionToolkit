import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author lfcounago
 */
public class DesempaquetarExamen {
    public static void main(String[] args)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException,
            InvalidKeyException, SignatureException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {

        if (args.length != 3) {
            mensajeAyuda();
            System.exit(1);
        }

        /* Cargar "provider" */
        Security.addProvider(new BouncyCastleProvider()); // Usa provider BC

        /* RECUPERAMOS EL PAQUETE */
        System.out.println("------Recuperando paquete-------");
        Paquete paquete = new Paquete(args[0]);

        // Recuperamos los datos del paquete
        byte[] examenCifrado = paquete.getContenidoBloque("Examen cifrado");
        byte[] claveSimetricaCifrada = paquete.getContenidoBloque("Clave secreta cifrada");
        byte[] fecha = paquete.getContenidoBloque("Fecha");
        byte[] sello = paquete.getContenidoBloque("Sello");

        System.out.println("Paquete recuperado correctamente");
        System.out.println("--------------------------------\n");

        /* RECUPERAMOS LA CLAVE PÚBLICA DE LA AUTORIDAD DE SELLADO */
        PublicKey KUautoridad = recuperClavePublica(args[1]);

        /* RECUPERAMOS LA CLAVE PRIVADA DEL PROFESOR */
        PrivateKey KRprofesor = recuperClavePrivada(args[2]);

        /* COMPRAMOS FIRMA RECIBIDA EN EL PAQUETE */
        System.out.println("----------Validando sello del alumno----------");
        boolean selloValido = validarSelloAlumno(fecha, sello, examenCifrado, KUautoridad, claveSimetricaCifrada);

        // Si el sello es válido procedemos a desencriptar el examen
        if (selloValido) {
            System.out.println("Sello del alumno validada correctamente");
            System.out.println("------------------------------------------\n");

            /* DESCIFRAMOS LA CLAVE SECRETA */
            System.out.println("--Descifrando clave simétrica--");
            byte[] claveDescifrada = descifrarClaveSimetrica(claveSimetricaCifrada, KRprofesor);
            System.out.println(" Clave descifrada correctamente");
            System.out.println("-------------------------------\n");

            /* DESCIFRAMOS EL EXAMEN */
            System.out.println("-----Descifrando el examen-----");
            byte[] examenDescifrado = descifradoDES(claveDescifrada, examenCifrado);
            System.out.println("Examen descifrado correctamente");
            System.out.println("-------------------------------\n");

            System.out.println("---Contenido del examen---\n");
            System.out.write(examenDescifrado);
            System.out.println("\n\n---Fin del examen---");
        } else {
            System.out.println("ERROR: Sello del alumno no validado");
            System.exit(1);
        }
    }

    /*
     * Función para validar el sello que generó el alumno cuando selló el examen
     * Devuelve un boolean de si está validad o no
     */
    public static boolean validarSelloAlumno(byte[] fecha, byte[] sello, byte[] examenCifrado,
            PublicKey KUautoridad, byte[] claveSimetricaCifrada)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {

        boolean validada = false;
        Signature firmaComprobar = Signature.getInstance("MD5withRSA", "BC");
        firmaComprobar.initVerify(KUautoridad);
        firmaComprobar.update(examenCifrado);
        firmaComprobar.update(claveSimetricaCifrada);
        firmaComprobar.update(fecha);
        if (firmaComprobar.verify(sello)) {
            System.out.println("El sello enviado y el recibido es el mismo");

            validada = true;
        } else {
            System.out.println("El sello enviado y el recibido no coinciden");
            System.exit(1);
        }

        return validada;
    }

    /*
     * Función para decifrar la clave simétrica (clave con la que se cifró el
     * examen) con la clave privada del profesor
     * Devolverá la clave descifrada
     */
    public static byte[] descifrarClaveSimetrica(byte[] clave, PrivateKey KRprofesor)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        // Obtener Cipher RSA
        Cipher cifradorRSA = Cipher.getInstance("RSA", "BC");
        // Poner CipherRSA en modo DESCIFRADO
        cifradorRSA.init(Cipher.DECRYPT_MODE, KRprofesor);
        byte[] claveDescifrada = cifradorRSA.doFinal(clave);

        return claveDescifrada;

    }

    /*
     * Función para descifrar el examen con la clave secreta
     * Devolvemos el examen descifrado
     */
    public static byte[] descifradoDES(byte[] claveDescifrada, byte[] examen) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        // Obtener Cipher DES
        Cipher cifradorDES = Cipher.getInstance("DES/ECB/PKCS5Padding");
        // Poner CipherDES en modo DESCIFRADO
        SecretKey claveDescifradaBack = new SecretKeySpec(claveDescifrada, 0, claveDescifrada.length, "DES");
        cifradorDES.init(Cipher.DECRYPT_MODE, claveDescifradaBack);
        byte[] examenDescifrado = cifradorDES.doFinal(examen);

        return examenDescifrado;

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
        System.out.println("Desempaquetador de exámenes");
        System.out.println(
                "\tSintaxis: java -cp \".;bcprov-jdk18on-176.jar\" DesempaquetarExamen paquete .\\autoridadSellado.publica .\\profesor.privada");
        System.out.println();
    }
}
