#pragma once
#include "CAsymmetricEncryptor.h"
#include <openssl/evp.h>
#include <openssl/pem.h>

namespace YSecurity
{
	class COpenSSLRSAEncryptor :
		public CAsymmetricEncryptor
	{

		EVP_PKEY_CTX * ctx;
		EVP_PKEY *key;
		const char * mdName;


		void getKeyFromBytes(unsigned char * key, int keyLen);

	public:

		COpenSSLRSAEncryptor() = delete;
		COpenSSLRSAEncryptor(unsigned char * key, int keyLen, const char * mdName);
		~COpenSSLRSAEncryptor();


		void setAsymmetricKey(unsigned char * key, int lenKey) override;
		void encryptData(unsigned char * data, int lenData) override;

		size_t getSizeBlock();
	};
}
