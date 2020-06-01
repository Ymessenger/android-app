#pragma once
#include "CAsymmetricDecryptor.h"
#include <openssl/evp.h>
#include <openssl/pem.h>
namespace YSecurity
{
	class COpenSSLRSADecryptor :
		public CAsymmetricDecryptor
	{
		EVP_PKEY_CTX * ctx;
		EVP_PKEY *key;
		const char * mdName;


		void getKeyFromBytes(unsigned char * key, int keyLen);
	public:
		COpenSSLRSADecryptor() = delete;
		~COpenSSLRSADecryptor();
		COpenSSLRSADecryptor(unsigned char * key, int keyLen, const char * mdName);

		void setAsymmetricKey(unsigned char * key, int lenKey) override;
		void decryptData(unsigned char * data, int lenData) override;

		size_t getSizeBlock();
	};

}