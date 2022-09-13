package ch.finecloud.peopledb.repository;

import ch.finecloud.peopledb.annotation.SQL;
import ch.finecloud.peopledb.model.Address;
import ch.finecloud.peopledb.model.CrudOperation;
import ch.finecloud.peopledb.model.Person;
import ch.finecloud.peopledb.model.Region;

import java.math.BigDecimal;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public class PeopleRepository extends CrudRepository<Person> {
    private AddressRepository addressRepository = null;
    public static final String SAVE_PERSON_SQL = """
        INSERT INTO PEOPLE
        (FIRST_NAME, LAST_NAME, DOB, SALARY, EMAIL, HOME_ADDRESS, BIZ_ADDRESS)
        VALUES(?, ?, ?, ?, ?, ?, ?)""";
    public static final String FIND_BY_ID_SQL = """
        SELECT 
        P.ID, P.FIRST_NAME, P.LAST_NAME, P.DOB, P.SALARY, P.HOME_ADDRESS,
        HOME.ID as HOME_ID, HOME.STREET_ADDRESS as HOME_STREET_ADDRESS, HOME.ADDRESS2 as HOME_ADDRESS2, HOME.CITY as HOME_CITY, HOME.STATE as HOME_STATE, HOME.POSTCODE as HOME_POSTCODE, HOME.COUNTY as HOME_COUNTY, HOME.REGION as HOME_REGION, HOME.COUNTRY as HOME_COUNTRY,
        BIZ.ID as BIZ_ID, BIZ.STREET_ADDRESS as BIZ_STREET_ADDRESS, BIZ.ADDRESS2 as BIZ_ADDRESS2, BIZ.CITY as BIZ_CITY, BIZ.STATE as BIZ_STATE, BIZ.POSTCODE as BIZ_POSTCODE, BIZ.COUNTY as BIZ_COUNTY, BIZ.REGION as BIZ_REGION, BIZ.COUNTRY as BIZ_COUNTRY,
        FROM PEOPLE AS P
        LEFT OUTER JOIN ADDRESSES AS HOME ON P.HOME_ADDRESS = HOME.ID
        LEFT OUTER JOIN ADDRESSES AS BIZ ON P.BIZ_ADDRESS = BIZ.ID
        WHERE P.ID=?""";
    public static final String FIND_ALL_SQL = "SELECT ID, FIRST_NAME, LAST_NAME, DOB, SALARY FROM PEOPLE";
    public static final String SELECT_COUNT_SQL = "SELECT COUNT(*) FROM PEOPLE";
    public static final String DELETE_SQL = "DELETE FROM PEOPLE WHERE ID=?";
    public static final String DELETE_IN_SQL = "DELETE FROM PEOPLE WHERE ID IN (:ids)";
    public static final String UPDATE_SQL = "UPDATE PEOPLE SET FIRST_NAME=?, LAST_NAME=?, DOB=?, SALARY=? WHERE ID=?";

    public PeopleRepository(Connection connection) {
        super(connection);
        addressRepository = new AddressRepository(connection);
    }

    @Override
    @SQL(value = SAVE_PERSON_SQL, operationType = CrudOperation.SAVE)
    void mapForSave(Person entity, PreparedStatement ps) throws SQLException {
        Address savedAddress = null;
        ps.setString(1, entity.getFirstName());
        ps.setString(2, entity.getLastName());
        ps.setTimestamp(3, convertODBtoTimeStamp(entity.getDob()));
        ps.setBigDecimal(4, entity.getSalary());
        ps.setString(5, entity.getEmail());
        associateAddressWithPerson(ps, entity.getHomeAddress(), 6);
        associateAddressWithPerson(ps, entity.getBusinessAddress(), 7);
    }

    private void associateAddressWithPerson(PreparedStatement ps, Optional<Address> address, int parameterIndex) throws SQLException {
        Address savedAddress;
        if (address.isPresent()) {
            savedAddress = addressRepository.save(address.get());
            ps.setLong(parameterIndex, savedAddress.id());
        } else {
            ps.setObject(parameterIndex, null);
        }
    }

    @Override
    @SQL(value = UPDATE_SQL, operationType = CrudOperation.UPDATE)
    void mapForUpdate(Person entity, PreparedStatement ps) throws SQLException {
        ps.setString(1, entity.getFirstName());
        ps.setString(2, entity.getLastName());
        ps.setTimestamp(3, convertODBtoTimeStamp(entity.getDob()));
        ps.setBigDecimal(4, entity.getSalary());
    }

    @Override
    @SQL(value = FIND_BY_ID_SQL, operationType = CrudOperation.FIND_BY_ID)
    @SQL(value = FIND_ALL_SQL, operationType = CrudOperation.FIND_ALL)
    @SQL(value = SELECT_COUNT_SQL, operationType =  CrudOperation.COUNT)
    @SQL(value = DELETE_IN_SQL, operationType = CrudOperation.DELETE_MANY)
    @SQL(value = DELETE_SQL, operationType = CrudOperation.DELETE_ONE)
    Person extractEntityFromResultSet(ResultSet rs) throws SQLException {
        long personID = rs.getLong("ID");
        String firstName = rs.getString("FIRST_NAME");
        String lastName = rs.getString("LAST_NAME");
        ZonedDateTime dob = ZonedDateTime.of(rs.getTimestamp("DOB").toLocalDateTime(), ZoneId.of("+0"));
        BigDecimal salary = rs.getBigDecimal("SALARY");
//        long homeAddressId = rs.getLong("HOME_ADDRESS");

        Address homeAddress = extractAddress(rs, "HOME_");
        Address bizAddress = extractAddress(rs, "BIZ_");

        Person person = new Person(personID, firstName, lastName, dob, salary);
        person.setHomeAddress(homeAddress);
        person.setBusinessAddress(bizAddress);
        return person;
    }

    private Address extractAddress(ResultSet rs, String aliasPrefix) throws SQLException {
        Long addrId = getValueByAlias(aliasPrefix + "ID", rs, Long.class);
        if (addrId == null) return null;
        String streetAddress = getValueByAlias(aliasPrefix + "STREET_ADDRESS", rs, String.class);
        String address2 = getValueByAlias(aliasPrefix + "ADDRESS2", rs, String.class);
        String city = getValueByAlias(aliasPrefix + "CITY", rs, String.class);
        String state = getValueByAlias(aliasPrefix + "STATE", rs, String.class);
        String postcode = getValueByAlias(aliasPrefix + "POSTCODE", rs, String.class);
        String county = getValueByAlias(aliasPrefix + "COUNTY", rs, String.class);
        Region region = Region.valueOf(getValueByAlias(aliasPrefix + "REGION", rs, String.class).toUpperCase());
        String country = getValueByAlias(aliasPrefix + "COUNTRY", rs, String.class);
        Address address = new Address(addrId, streetAddress, address2, city, state, postcode, country, county, region);
        return address;
    }

    private <T> T getValueByAlias(String alias, ResultSet rs, Class<T> clazz) throws SQLException {
        int columnCount = rs.getMetaData().getColumnCount();
        for (int colIdX = 1; colIdX < columnCount; colIdX++) {
            if (alias.equals(rs.getMetaData().getColumnLabel(colIdX))) {
                return (T) rs.getObject(colIdX);
            }
        }
        throw new SQLException(String.format("Column not found for alias: '%s'", alias));
    }

    private Timestamp convertODBtoTimeStamp(ZonedDateTime dob) {
        return Timestamp.valueOf(dob.withZoneSameInstant(ZoneId.of("+0")).toLocalDateTime());
    }
}
