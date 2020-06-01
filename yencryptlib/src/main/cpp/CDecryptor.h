#pragma once
#include "CError.h"
namespace YSecurity
{
	class CDecryptor : public CError
	{
	protected:
		unsigned char * outData;
		size_t lenOutData;
	public:
		CDecryptor();
		virtual ~CDecryptor();

		virtual void decryptData(unsigned char * data, int lenData) = 0;

		size_t getLenDecryptedData();
		unsigned char * getDecryptedData();
	};
}