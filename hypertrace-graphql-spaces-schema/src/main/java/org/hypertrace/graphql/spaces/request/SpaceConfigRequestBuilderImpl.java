package org.hypertrace.graphql.spaces.request;

import java.util.Map;
import javax.inject.Inject;
import lombok.Value;
import lombok.experimental.Accessors;
import org.hypertrace.core.graphql.common.utils.attributes.AttributeScopeStringTranslator;
import org.hypertrace.core.graphql.context.GraphQlRequestContext;
import org.hypertrace.core.graphql.deserialization.ArgumentDeserializer;
import org.hypertrace.graphql.spaces.schema.shared.SpaceConfigRuleAttributeValueRule;
import org.hypertrace.graphql.spaces.schema.shared.SpaceConfigRuleDefinition;
import org.hypertrace.graphql.spaces.schema.shared.SpaceConfigRuleType;

class SpaceConfigRequestBuilderImpl implements SpaceConfigRequestBuilder {

  private final ArgumentDeserializer argumentDeserializer;
  private final AttributeScopeStringTranslator scopeStringTranslator;

  @Inject
  SpaceConfigRequestBuilderImpl(
      ArgumentDeserializer argumentDeserializer,
      AttributeScopeStringTranslator scopeStringTranslator) {
    this.argumentDeserializer = argumentDeserializer;
    this.scopeStringTranslator = scopeStringTranslator;
  }

  @Override
  public SpaceConfigRuleCreationRequest buildCreationRequest(
      GraphQlRequestContext requestContext, Map<String, Object> arguments) {
    return new SpaceConfigRuleCreationRequestImpl(
        requestContext,
        this.argumentDeserializer
            .deserializeObject(arguments, SpaceConfigRuleDefinition.class)
            .map(this::normalizeDefinition)
            .orElseThrow());
  }

  /**
   * This is required to translate external GQL scopes into internal platform ones. If we can find a
   * way to do that in the arg deserializer, that'd be ideal but jackson makes it difficult to get
   * at the translator instance we need to get from guice.
   */
  private SpaceConfigRuleDefinition normalizeDefinition(SpaceConfigRuleDefinition definition) {
    return new NormalizedSpaceConfigRuleDefinition(
        definition.type(), this.normalizeAttributeValueRule(definition.attributeValueRule()));
  }

  private SpaceConfigRuleAttributeValueRule normalizeAttributeValueRule(
      SpaceConfigRuleAttributeValueRule attributeValueRule) {
    return new NormalizedSpaceConfigRuleAttributeValueRule(
        this.scopeStringTranslator.fromExternal(attributeValueRule.attributeScope()),
        attributeValueRule.attributeKey());
  }

  @Value
  @Accessors(fluent = true)
  private static class SpaceConfigRuleCreationRequestImpl
      implements SpaceConfigRuleCreationRequest {
    GraphQlRequestContext context;
    SpaceConfigRuleDefinition ruleDefinition;
  }

  @Value
  @Accessors(fluent = true)
  private static class NormalizedSpaceConfigRuleDefinition implements SpaceConfigRuleDefinition {
    SpaceConfigRuleType type;
    SpaceConfigRuleAttributeValueRule attributeValueRule;
  }

  @Value
  @Accessors(fluent = true)
  private static class NormalizedSpaceConfigRuleAttributeValueRule
      implements SpaceConfigRuleAttributeValueRule {
    String attributeScope;
    String attributeKey;
  }
}