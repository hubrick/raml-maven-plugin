package ${package};

<#list imports as class>
import ${class};
</#list>

@${classRef("org.springframework.web.bind.annotation.RestController")}
@${classRef("org.springframework.web.bind.annotation.RequestMapping")}("${raml.baseUri!"/"}")
public interface ${classRef(resourceClassName)} {
    <#list actionDefinitions as actionDefinition>

    @${classRef("org.springframework.web.bind.annotation.RequestMapping")}(value = "${actionDefinition.action.resource.relativeUri}", method = ${classRef("org.springframework.web.bind.annotation.RequestMethod")}.${actionDefinition.action.type.name()})
    <#if actionDefinition.responseBodySchema??>${schemaClassRef(actionDefinition.responseBodySchema)}<#else>void</#if> ${actionMethodName(actionDefinition.action)}(<#if actionDefinition.requestBodySchema??>@RequestBody ${schemaClassRef(actionDefinition.requestBodySchema)} ${javaName(actionDefinition.requestBodySchema)}<#if actionDefinition.uriParameterDefinitions?has_content>, </#if></#if><#list actionDefinition.uriParameterDefinitions as uriParamDef>@${classRef("org.springframework.web.bind.annotation.PathVariable")}("${uriParamDef.name}") ${classRef(uriParamDef.javaType)} ${uriParamDef.name}<#sep>, </#sep></#list>);
    </#list>

}
