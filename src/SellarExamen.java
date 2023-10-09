import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.time.LocalDateTime;

/**
 *
 * @author lfcounago
 */
public class SellarExamen {

    public static void main(String[] args)
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidKeySpecException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, SignatureException {

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
        byte[] firmaCifrada = paquete.getContenidoBloque("Firma cifrada");
        byte[] examenCifrado = paquete.getContenidoBloque("Examen cifrado");
        byte[] claveSimetricaCifrada = paquete.getContenidoBloque("Clave secreta cifrada");
        System.out.println("Paquete recuperado correctamente");
        System.out.println("--------------------------------\n");

        /* RECUPERAR KU DEL ALUMNO */
        // Obtenemos la clave pública del profesor
        PublicKey KUalumno = recuperClavePublica(args[1]);

        /* COMPROBAMOS LA VALIDEZ DE LA FIRMA */
        // Validamos la firma del alumno
        System.out.println("---------Validando firma del alumno---------");
        boolean firmaValida = validarFirmaAlumno(firmaCifrada, examenCifrado, KUalumno, claveSimetricaCifrada);

        // Si la firma es válida procedemos a generar el timestamp y el sello
        if (firmaValida) {
            System.out.println("Firma del alumno validada correctamente");
            System.out.println("--------------------------------------------\n");

            /* OBTENER TIMESTAMP */
            // Obtenemos el timestamp la fecha y hora de entrega
            System.out.println("-------Generando timestamp------");
            byte[] timeStamp = LocalDateTime.now().toString().getBytes();
            System.out.println("Timestamp generado correctamente");
            System.out.println("--------------------------------\n");

            /* RECUPER KR DE LA AUTORIDAD DE SELLADO */
            PrivateKey KRautoridad = recuperClavePrivada(args[2]);

            /* GENERAMOS EL SELLO */
            System.out.println("-------Generando sello------");
            byte[] sello = generarSello(timeStamp, examenCifrado, claveSimetricaCifrada, firmaCifrada,
                    KRautoridad);
            System.out.println("Sello generado correctamente");
            System.out.println("----------------------------\n");

            /* ACTUALIZAR EL PAQUETE CON EL TIMESTAMP Y EL SELLO */
            System.out.println("-------Actualizando paquete------");
            paquete.anadirBloque("Fecha", timeStamp);
            paquete.anadirBloque("Sello", sello);
            paquete.escribirPaquete(args[0]);
            System.out.println("Paquete actualizado correctamente");
            System.out.println("---------------------------------\n");

        } else {
            System.out.println("ERROR: Firma del alumno no validada");
            System.exit(1);
        }
    }

    /*
     * Función para validar la firma que generó el alumno en EmpaquetarExamen y que
     * se incorporó en el paquete
     * Devuelve un boolean de si está validad o no
     */
    public static boolean validarFirmaAlumno(byte[] firmaCifrada, byte[] examenCifrado, PublicKey KUalumno,
            byte[] claveSimetricaCifrada)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {

        boolean validada = false;
        Signature firmaComprobar = Signature.getInstance("MD5withRSA", "BC");
        firmaComprobar.initVerify(KUalumno);
        firmaComprobar.update(examenCifrado);
        firmaComprobar.update(claveSimetricaCifrada);
        if (firmaComprobar.verify(firmaCifrada)) {
            System.out.println("El resumen enviado y el recibido es el mismo");
            validada = true;
        } else {
            System.out.println("El resumen enviado y el recibido no coinciden");
        }

        return validada;
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

    /*
     * Función para generar el sellado del examen
     * Devuelve el sello que será introducido en el paquete
     */
    public static byte[] generarSello(byte[] timeStamp, byte[] examenCifrado, byte[] claveSimetricaCifrada,
            byte[] firmaCifrada, PrivateKey clavePrivadaSellado)
            throws NoSuchAlgorithmException, NoSuchProviderException, SignatureException, InvalidKeyException {

        Signature sello = Signature.getInstance("MD5withRSA", "BC");
        sello.initSign(clavePrivadaSellado);
        sello.update(examenCifrado);
        sello.update(claveSimetricaCifrada);
        sello.update(timeStamp);
        byte[] selloFinal = sello.sign();

        return selloFinal;
    }

    public static void mensajeAyuda() {
        System.out.println("Sellador de exámenes");
        System.out.println(
                "\tSintaxis: java -cp \".;bcprov-jdk18on-176.jar\" SellarExamen paquete .\\alumno.publica .\\autoridadSellado.privada");
        System.out.println();
    }
}
