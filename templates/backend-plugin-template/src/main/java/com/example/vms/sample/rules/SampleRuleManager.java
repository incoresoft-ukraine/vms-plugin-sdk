package com.example.vms.sample.rules;

import com.example.vms.sample.db.CategoriesRepository;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.incoresoft.middleware.vms.rules.RegisterStatus;
import com.incoresoft.middleware.vms.rules.RuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the single {@link SampleRuleType} instance and its registration.
 *
 * <p>The instance must be the same object for registration, lookup and unregistration: the host
 * keys rules by {@code getTypeId()}, and {@code getRulesOfType(...)} is called with the very
 * instance the plugin registered.
 *
 * <p>Registration happens in {@code PluginExtension.init()} and unregistration in
 * {@code terminate()}. Skipping the second leaves a dead rule type behind after the plugin is
 * stopped, and the host then fails to deserialize rules of that type.
 */
@Singleton
public class SampleRuleManager {
    private static final Logger log = LoggerFactory.getLogger(SampleRuleManager.class);

    private final RuleManager ruleManager;
    private final SampleRuleType ruleType;

    @Inject
    public SampleRuleManager(RuleManager ruleManager, CategoriesRepository categories) {
        this.ruleManager = ruleManager;
        this.ruleType = new SampleRuleType(categories);
    }

    public void registerRules() {
        RegisterStatus status = ruleManager.registerRuleType(ruleType);
        log.info("Sample rule type registered: {}", status);
    }

    public void unregisterRules() {
        ruleManager.unregisterRuleType(ruleType);
    }

    public SampleRuleType getRuleType() {
        return ruleType;
    }
}
