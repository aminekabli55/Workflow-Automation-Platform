package com.amine.common.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @Getter
@Setter @AllArgsConstructor
public class WorkflowDto {
    private Long id;
    private String name;
    private String description;
    
}
