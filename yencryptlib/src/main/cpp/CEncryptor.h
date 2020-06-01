#pragma once
#include "CError.h"
namespace YSecurity
{
	class CEncryptor : public CError
	{
	protected:
		unsigned char * outData;
		size_t lenOutData;

	public:
		CEncryptor();
		virtual ~CEncryptor();
		virtual void encryptData(unsigned char * data, int lenData) = 0;

		size_t getLenEcryptedData();
		unsigned char * getEcryptedData();
	};
}

