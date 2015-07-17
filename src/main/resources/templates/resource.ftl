package ${package};

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("${raml.baseUri!"/"}")
public interface ${className} {

    <#list actions as action>
    @RequestMapping(value = "${action.resource.relativeUri}", method = RequestMethod.${action.type.name()})
    public ${actionMethodName(action)}(<#list action.resource.resolvedUriParameters?keys as paramName>
            <#assign param=action.resource.resolvedUriParameters[paramName]>
            @PathVariable("${paramName}") ${javaType(param.type)} ${paramName}<#sep>, </#sep>
        </#list>);
    </#list>

}
