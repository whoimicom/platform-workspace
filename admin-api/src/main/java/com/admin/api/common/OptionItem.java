package com.admin.api.common;

import java.util.List;

/**
 * 下拉/树形选项：{ value, label, children }。
 */
public record OptionItem(String value, String label, List<OptionItem> children) {

    public OptionItem(String value, String label) {
        this(value, label, List.of());
    }
}
