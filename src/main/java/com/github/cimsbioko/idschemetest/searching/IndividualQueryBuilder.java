package com.github.cimsbioko.idschemetest.searching;

import java.util.Map;

/**
 * Builds a lucene query string for indexed individuals.
 */
public class IndividualQueryBuilder implements QueryBuilder {

    @Override
    public String buildQuery(Map<String, Object> params) {

        StringBuilder qstr = new StringBuilder();

        if (params.containsKey("dip")) {
            // We boost dip so it dominates score
            qstr.append(String.format("dip:%s^4", params.get("dip")));
        }

        if (params.containsKey("name")) {
            if (qstr.length() > 0)
                qstr.append(" ");
            qstr.append("name:(");
            String nameVal = params.get("name").toString();
            for (String name : nameVal.split("\\s+")) {
                qstr.append(String.format("%s~ ", name));
            }
            qstr.deleteCharAt(qstr.length() - 1);
            qstr.append(")");
        }

        if (params.containsKey("age")) {
            if (qstr.length() > 0)
                qstr.append(" ");
            int age = Integer.parseInt(params.get("age").toString());
            qstr.append(String.format("age:[%d TO %d]", age - 3, age + 3));
        }

        if (params.containsKey("phone")) {
            if (qstr.length() > 0)
                qstr.append(" ");
            String cleansedPhone = params.get("phone").toString().replaceAll("[^0-9]", "");
            if (cleansedPhone.length() >= 7) {
                qstr.append(String.format("phone:%s~", cleansedPhone));
            } else {
                qstr.append(String.format("phone:*%s", cleansedPhone));
            }
        }

        if (params.containsKey("district")) {
            if (qstr.length() > 0)
                qstr.append(" ");
            qstr.append(String.format("district:%s~", params.get("district")));
        }

        if (params.containsKey("community")) {
            if (qstr.length() > 0)
                qstr.append(" ");
            qstr.append(String.format("community:%s~", params.get("community")));
        }

        if (params.containsKey("headName")) {
            if (qstr.length() > 0)
                qstr.append(" ");
            qstr.append("headName:(");
            String nameVal = params.get("headName").toString();
            for (String name : nameVal.split("\\s+")) {
                qstr.append(String.format("%s~ ", name));
            }
            qstr.deleteCharAt(qstr.length() - 1);
            qstr.append(")");
        }

        return qstr.toString();
    }
}
