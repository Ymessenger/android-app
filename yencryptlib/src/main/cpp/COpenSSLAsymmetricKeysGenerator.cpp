#include "COpenSSLAsymmetricKeysGenerator.h"
#include <string>
#include <exception>

namespace YSecurity
{
	COpenSSLAsymmetricKeysGenerator::COpenSSLAsymmetricKeysGenerator(int idAlgoritm) :ctx(NULL), pkey(NULL), pubKey(nullptr), privKey(nullptr)
	{
		setAlgoritm(idAlgoritm);
	}

	COpenSSLAsymmetricKeysGenerator::~COpenSSLAsymmetricKeysGenerator()
	{
		if (ctx != NULL)
		{
			EVP_PKEY_CTX_free(ctx);
			//OPENSSL_free(ctx);
		}

		if (pkey != NULL)
		{
			EVP_PKEY_free(pkey);
			//OPENSSL_free(pkey);
		}

		if (pubKey)
		{
			delete[] pubKey;
		}

		if (privKey)
		{
			delete[] privKey;
		}
	}

	void COpenSSLAsymmetricKeysGenerator::generate()
	{
		if (EVP_PKEY_keygen(ctx, &pkey) != 1)
		{
			throw "Error generate";
		}
	}


	unsigned char * COpenSSLAsymmetricKeysGenerator::getPublicKey()
	{
		if (pkey == NULL)
		{
			throw "No keys";
		}

		if (!pubKey)
		{
			throw "Not found PEM";
		}

		return pubKey;
	}

	unsigned char * COpenSSLAsymmetricKeysGenerator::getPrivateKey()
	{
		if (pkey == NULL)
		{
			throw "No keys";
		}

		if (!privKey)
		{
			throw "Not found PEM";
		}

		return privKey;

	}


	int COpenSSLAsymmetricKeysGenerator::getLenBufferPublicKey()
	{
		if (pkey == NULL)
		{
			throw "No keys";
		}

		if (!pubKey)
		{
			throw "Not found PEM";
		}

		return lenPubKey;
	}

	int COpenSSLAsymmetricKeysGenerator::getLenBufferPrivateKey()
	{
		if (pkey == NULL)
		{
			throw "No keys";
		}

		if (!privKey)
		{
			throw "Not found PEM";
		}

		return lenPrivKey;
	}


	void COpenSSLAsymmetricKeysGenerator::setAlgoritm(int id)
	{
		ctx = EVP_PKEY_CTX_new_id(id, NULL);
		if (ctx == NULL)
		{
			throw "Error create context";
		}

		if (EVP_PKEY_keygen_init(ctx) != 1)
		{
			throw "Error init context";
		}
	}

	EVP_PKEY * COpenSSLAsymmetricKeysGenerator::getEVP_PKEY()
	{
		if (this->pkey)
			return this->pkey;
		else
			return nullptr;
	}
}
