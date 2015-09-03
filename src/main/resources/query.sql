select
	i.extId,
	case i.dip when 0 then null else i.dip end as dip,
	case i.firstName when 'null' then null else i.firstName end as firstName,
	case i.middleName when 'null' then null else i.middleName end as middleName,
	case i.lastName when 'null' then null else i.lastName end as lastName,
	date_format(now(), '%Y') - date_format(i.dob, '%Y') - (date_format(now(), '00-%m-%d') < date_format(i.dob, '00-%m-%d')) as age,
	case i.phoneNumber when 'null' then null else i.phoneNumber end as phoneNumber,
	case i.otherPhoneNumber when 'null' then null else i.otherPhoneNumber end as otherPhoneNumber,
	case i.pointOfContactPhoneNumber when 'null' then null else i.pointOfContactPhoneNumber end as pointOfContactPhoneNumber,
	case i.pointOfContactName when 'null' then null else i.pointOfContactName end as pointOfContactName,
	x.district as district,
	x.community as community,
	x.firstName as hhFirstName,
	x.middleName as hhMiddleName,
	x.lastName as hhLastName
from
	individual i
left join
	x on i.uuid = x.individual_uuid
where
	i.deleted != 1