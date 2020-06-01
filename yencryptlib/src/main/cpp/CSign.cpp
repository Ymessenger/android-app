#include "CSign.h"

namespace YSecurity
{
	CSign::~CSign()
	{
		if (this->sign)
		{
			delete[] this->sign;
		}
	}

	size_t CSign::getLenSign()
	{
		if (!this->_isError)
		{
			if (this->sign)
			{
				return this->lenSign;
			}
			else
			{
				this->_isError = true;
				this->codeError = 3001;
				this->lastError = "Not found signed data";
			}
		}
		return 0;
	}

	unsigned char * CSign::getSign()
	{
		if (!this->_isError)
		{
			if (this->sign)
			{
				return this->sign;
			}
			else
			{
				this->_isError = true;
				this->codeError = 3001;
				this->lastError = "Not found signed data";
			}
		}
		return nullptr;
	}

}
