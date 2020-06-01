#pragma once
#include "CVerify.h"
#include <openssl/evp.h>
namespace YSecurity
{
	class CVerifyOpenSSLRSA :
		public CVerify
	{
		EVP_MD_CTX * mdctx;
		EVP_PKEY * pkey;
		const char * mdName;

		void setKeyFromBytes(unsigned char * key, size_t keyLen);
	public:
		CVerifyOpenSSLRSA(unsigned char * sign, size_t sLen, unsigned char * key, size_t kLen, const char * mdName);
		virtual ~CVerifyOpenSSLRSA();

		void setPublicKey(unsigned char * sign, size_t sLen) override;
		void addMsg(unsigned char * msg, size_t lenMsg) override;
		void verify() override;

	};
}

