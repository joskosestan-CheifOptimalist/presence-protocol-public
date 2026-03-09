package com.presenceprotocol.core.crypto

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.util.Base64

object EphemeralKeys {
    private const val KEY_SIZE = 256

    fun generate(): Pair<KeyPair, String> {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(KEY_SIZE, SecureRandom())

        val pair = generator.generateKeyPair()
        val pub = Base64.getEncoder().encodeToString(pair.public.encoded)

        return Pair(pair, pub)
    }

    fun publicBase64(pair: KeyPair): String {
        return Base64.getEncoder().encodeToString(pair.public.encoded)
    }

    fun signBase64(privateKey: PrivateKey, message: ByteArray): String {
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(privateKey)
        signer.update(message)
        return Base64.getEncoder().encodeToString(signer.sign())
    }

    fun verifyBase64(publicKey: PublicKey, message: ByteArray, signatureBase64: String): Boolean {
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(message)
        return verifier.verify(Base64.getDecoder().decode(signatureBase64))
    }
}
