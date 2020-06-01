#pragma once
#include "CAsymmetricKeysGenerator.h"
#include <openssl/evp.h>
#include <openssl/pem.h>

namespace YSecurity
{
	class COpenSSLAsymmetricKeysGenerator :
		public CAsymmetricKeysGenerator
	{
	protected:
		EVP_PKEY_CTX *ctx;
		EVP_PKEY *pkey;

		unsigned char * pubKey;
		int lenPubKey;

		unsigned char * privKey;
		int lenPrivKey;

	public:
		COpenSSLAsymmetricKeysGenerator() = delete;
		COpenSSLAsymmetricKeysGenerator(int idAlgoritm);
		virtual ~COpenSSLAsymmetricKeysGenerator();


		void generate() override;

		unsigned char * getPublicKey() override;
		unsigned char * getPrivateKey() override;


		int getLenBufferPublicKey() override;
		int getLenBufferPrivateKey() override;

		virtual bool checkKeys() = 0;
		void setAlgoritm(int id);

		EVP_PKEY * getEVP_PKEY();

	};

}