#include "CEncryptor.h"

namespace YSecurity
{

	CEncryptor::CEncryptor()
	{
	}


	CEncryptor::~CEncryptor()
	{
		if (outData)
		{
			delete[] outData;
		}
	}



	size_t CEncryptor::getLenEcryptedData()
	{
		if (!this->_isError)
		{
			if (this->outData)
			{
				return this->lenOutData;
			}
			else
			{
				this->_isError = true;
				this->codeError = 3001;
				this->lastError = "Not found encrypted data";
			}
		}
		return 0;
	}

	unsigned char * CEncryptor::getEcryptedData()
	{
		if (!this->_isError)
		{
			if (this->outData)
			{
				return this->outData;
			}
			else
			{
				this->_isError = true;
				this->codeError = 3001;
				this->lastError = "Not found encrypted data";
			}
		}
		return nullptr;
	}
}