#pragma once
#include "CSign.h"
#include <openssl/evp.h>
namespace YSecurity
{
	class CSignOpenSSLRSA :
		public CSign
	{
		EVP_MD_CTX * mdctx;
		EVP_PKEY * pkey;
		const char * mdName;

		void setKeyFromBytes(unsigned char * key, size_t keyLen);
	public:
		CSignOpenSSLRSA(unsigned char * key, size_t keyLen, const char * mdName);
		~CSignOpenSSLRSA();

		void addMsg(unsigned char * msg, size_t lenMsg) override;
		void subscribe() override;
		void SetPrivetKey(unsigned char * key, size_t keyLen) override;
	};

}