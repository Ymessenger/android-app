#include "YTime.h"
#include <ctime>

YSecurity::date YSecurity::YTime::getDateTime()
{
	return time(0);
}

bool YSecurity::YTime::isAlive(YSecurity::date timeCreate, YSecurity::uint_32 timeLife)
{
	auto now = time(0);

	return timeLife > (now - timeCreate);
}

bool YSecurity::YTime::isCorrectKey(YSecurity::date timeCreateMsg, YSecurity::date timeCreateKey, YSecurity::uint_32 timeLifeKey, YSecurity::uint_32 limit)
{
	if ((timeCreateMsg <= (timeCreateKey + timeLifeKey)) && (timeCreateMsg >= timeCreateKey - limit))
	{
		return true;
	}

	return false;
}
