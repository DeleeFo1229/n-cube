package com.cedarsoftware.ncube;

import com.cedarsoftware.util.CaseInsensitiveMap;
import groovy.util.MapEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains information about the rule execution.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class RuleInfo extends CaseInsensitiveMap<String, Object>
{
    public RuleInfo()
    {
        put(RuleMetaKeys.NUM_RESOLVED_CELLS.name(), 0L);
        put(RuleMetaKeys.RULES_EXECUTED.name(), new ArrayList<MapEntry>());
    }

    public long getNumberOfRulesExecuted()
    {
        return (Long) get(RuleMetaKeys.NUM_RESOLVED_CELLS.name());
    }

    public void addToRulesExecuted(long count)
    {
        put(RuleMetaKeys.NUM_RESOLVED_CELLS.name(), getNumberOfRulesExecuted() + count);
    }

    public List<MapEntry> getRuleExecutionTrace()
    {
        return (List<MapEntry>)get(RuleMetaKeys.RULES_EXECUTED.name());
    }

    public void ruleStopThrown()
    {
        put(RuleMetaKeys.RULE_STOP.name(), Boolean.TRUE);
    }

    public boolean wasRuleStopThrown()
    {
        return containsKey(RuleMetaKeys.RULE_STOP.name()) && (Boolean.TRUE == get(RuleMetaKeys.RULE_STOP.name()));
    }
}
