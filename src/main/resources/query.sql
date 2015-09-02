select
	extId,
	case dip when 0 then null else dip end as dip,
	case firstName when 'null' then null else firstName end as firstName,
	case middleName when 'null' then null else middleName end as middleName,
	case lastName when 'null' then null else lastName end as lastName,
	date_format(now(), '%Y') - date_format(dob, '%Y') - (date_format(now(), '00-%m-%d') < date_format(dob, '00-%m-%d')) as age,
	case phoneNumber when 'null' then null else phoneNumber end as phoneNumber,
	case otherPhoneNumber when 'null' then null else otherPhoneNumber end as otherPhoneNumber,
	case pointOfContactPhoneNumber when 'null' then null else pointOfContactPhoneNumber end as pointOfContactPhoneNumber
from
	individual i
where
	i.deleted != 1