/*
 * Created by Maxim Pogorelov <pogorelovm23@gmail.com>, 10/01/17.
 */

package com.icodici.universa.contract.permissions;

import com.icodici.universa.contract.Contract;
import com.icodici.universa.contract.roles.Role;
import net.sergeych.biserializer.BiDeserializer;
import net.sergeych.biserializer.BiSerializer;
import net.sergeych.biserializer.BiType;
import net.sergeych.biserializer.DefaultBiMapper;
import net.sergeych.diff.ChangedItem;
import net.sergeych.diff.Delta;
import net.sergeych.diff.ListDelta;
import net.sergeych.diff.MapDelta;
import net.sergeych.tools.Binder;

import java.util.*;


@BiType(name = "ModifyDataPermission")
public class ModifyDataPermission extends Permission {

    public static final String FIELD_NAME = "modify_data";

    private Map<String, List<String>> fields = new HashMap<>();

    public ModifyDataPermission() {
    }

    public ModifyDataPermission(Role role, Binder params) {
        super(FIELD_NAME, role, params);
        Object fields = params.get("fields");
        if (fields != null && fields instanceof Map) {
            this.fields.putAll((Map) fields);
        }
    }

    public ModifyDataPermission addField(String fieldName, List<String> values) {
        this.fields.put(fieldName, values);
        return this;
    }

    public void addAllFields(Map<String, List<String>> fields) {
        this.fields.putAll(fields);
    }

    /**
     * checkChanges processes the map of changes with the list of fields with predefined data options for a role.
     *
     * @param contract     source (valid) contract
     * @param changedContract is contract for checking
     * @param stateChanges map of changes, see {@link Delta} for details
     */
    @Override
    public void checkChanges(Contract contract, Contract changedContract, Map<String, Delta> stateChanges) {
        Delta data = stateChanges.get("data");
        if (data != null && data instanceof MapDelta) {
            Map mapChanges = ((MapDelta) data).getChanges();
            mapChanges.keySet().removeIf(key -> {
                Object changed = mapChanges.get(key);

                Object value = "";

                if (changed != null && changed instanceof ChangedItem) {
                    value = ((ChangedItem) mapChanges.get(key)).newValue();
                }

                boolean containsField = this.fields.containsKey(key);

                List<String> foundField = this.fields.get(key);


                return (containsField && foundField == null) ||
                        (foundField != null && foundField.contains(value) || isEmptyOrNull(foundField, value));
            });
        }

        // check references modify
        // TODO: this is hack, shouldn't access directly to references

        boolean containsField = this.fields.containsKey("references");
        List<String> foundField = this.fields.get("references");

        Delta references = stateChanges.get("references");
        if (references != null && references instanceof ListDelta) {
            Map mapChanges = ((ListDelta) references).getChanges();
            mapChanges.keySet().removeIf(key -> {
                Object changed = mapChanges.get(key);

                Object value = "";

                if (changed != null && changed instanceof ChangedItem) {
                    value = ((ChangedItem) mapChanges.get(key)).newValue();
                }


                return (containsField && foundField == null) ||
                        (foundField != null && foundField.contains(value) || isEmptyOrNull(foundField, value));
            });
        }
    }

    private boolean isEmptyOrNull(List<String> data, Object value) {
        return (value == null || "".equals(value)) && (data.contains(null) || data.contains(""));
    }

    public Map<String, List<String>> getFields() {
        return fields;
    }

    @Override
    public Binder serialize(BiSerializer serializer) {
        Binder results = super.serialize(serializer);
        results.put("fields", serializer.serialize(this.fields));
        return results;
    }

    @Override
    public void deserialize(Binder data, BiDeserializer deserializer) {
        super.deserialize(data, deserializer);
        Object fields = data.get("fields");
        if (fields != null && fields instanceof Map) {
            this.fields.putAll((Map) fields);
        }
    }

    static {
        DefaultBiMapper.registerClass(ModifyDataPermission.class);
    }
}