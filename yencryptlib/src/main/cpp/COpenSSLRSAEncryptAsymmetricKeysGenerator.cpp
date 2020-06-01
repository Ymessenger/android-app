#include "COpenSSLRSAEncryptAsymmetricKeysGenerator.h"
#include<string>


namespace YSecurity
{
	void COpenSSLRSAEncryptAsymmetricKeysGenerator::freeKey()
	{
		if (pubKey)
			OPENSSL_free(pubKey);
		if (privKey)
			OPENSSL_free(privKey);

		privKey = NULL;
		pubKey = NULL;
	}
	COpenSSLRSAEncryptAsymmetricKeysGenerator::COpenSSLRSAEncryptAsymmetricKeysGenerator(int lenKey) :
		COpenSSLAsymmetricKeysGenerator(EVP_PKEY_RSA)
	{
		EVP_PKEY_CTX_set_rsa_keygen_bits(ctx, lenKey);
		EVP_PKEY_CTX_set_rsa_padding(ctx, RSA_NO_PADDING);
		
		pubKey = NULL;
		privKey = NULL;

	}

	COpenSSLRSAEncryptAsymmetricKeysGenerator::~COpenSSLRSAEncryptAsymmetricKeysGenerator()
	{
		freeKey();
	}


	bool COpenSSLRSAEncryptAsymmetricKeysGenerator::checkKeys()
	{
		if (!pkey) throw "No keys";

		auto t = EVP_PKEY_get1_RSA(pkey);
		if (!t)
		{
			throw "No keys RSA";
		}

		auto ch = RSA_check_key(t);
		return ch == 1;
	}

	void COpenSSLRSAEncryptAsymmetricKeysGenerator::generate()
	{
		COpenSSLAsymmetricKeysGenerator::generate();

		lenPubKey = i2d_PUBKEY(pkey, &pubKey);

		auto pkcs8 = EVP_PKEY2PKCS8(pkey);
		lenPrivKey = i2d_PKCS8_PRIV_KEY_INFO(pkcs8, &privKey);
		PKCS8_PRIV_KEY_INFO_free(pkcs8);
		/*auto t = EVP_PKEY_get1_RSA(pkey);
		if (!t)
		{
			throw "No keys RSA";
		}
		freeKey();
		*/
		//lenPubKey = i2d_RSAPublicKey(t, &pubKey);

		//lenPrivKey = i2d_RSAPrivateKey(t, &privKey);

		if (lenPrivKey <= 0 || lenPubKey <= 0)
		{
			throw "No keys";
		}
	}

	void COpenSSLRSAEncryptAsymmetricKeysGenerator::setLenKey(int len)
	{
		EVP_PKEY_CTX_set_rsa_keygen_bits(ctx, len);
	}
}
