/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/common/components/TabComponent", [
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/util/UIUtils",

    // jquery dependencies
    "bootstrap-tabdrop"
], ($, _, Backbone, UIUtils) => {
    function has (attribute, tab) {
        if (!tab[attribute]) {
            throw new TypeError(`[TabComponent] Expected all items within 'tabs' to have a '${attribute}' attribute.`);
        }
        if (!_.isString(tab[attribute])) {
            throw new TypeError(`[TabComponent] Expected all items within 'tabs' to have String '${attribute}'s.`);
        }

        return true;
    }

    const TabComponent = Backbone.View.extend({
        template: "templates/admin/views/realms/services/TabComponentTemplate.html",
        events: {
            "show.bs.tab ul.nav.nav-tabs a": "handleTabClick"
        },
        initialize (options) {
            if (!(options.tabs instanceof Array)) {
                throw new TypeError("[TabComponent] \"tabs\" argument is not an Array.");
            }
            if (_.isEmpty(options.tabs)) {
                throw new TypeError("[TabComponent] \"tabs\" argument is an empty Array.");
            }
            _(options.tabs)
                .each(_.partial(has, "id"))
                .each(_.partial(has, "title"))
                .value();

            this.options = options;
        },
        getTabBody () {
            return this.tabBody;
        },
        getTabBodyElement () {
            return this.$el.find("[data-tab-panel]");
        },
        getTabFooter () {
            return this.tabFooter;
        },
        getTabFooterElement () {
            return this.$el.find("[data-tab-footer]");
        },
        getTabId () {
            return this.currentTabId;
        },
        setTabId (id) {
            this.currentTabId = id;
            this.$el.find(`[data-tab-id="${id}"]`).tab("show");
        },
        handleTabClick (event) {
            this.currentTabId = $(event.currentTarget).data("tab-id");

            this.getTabBodyElement().empty();
            this.tabBody = this.options.createTabBody(this.currentTabId);
            if (this.tabBody) {
                this.tabBody.setElement(this.getTabBodyElement());
                this.tabBody.render();
            }

            this.getTabFooterElement().empty();
            this.tabFooter = this.options.createTabFooter(this.currentTabId);
            if (this.tabFooter) {
                this.tabFooter.setElement(this.getTabFooterElement());
                this.tabFooter.render();
            }
        },
        render () {
            UIUtils.fillTemplateWithData(this.template, this.options, (html) => {
                this.$el.html(html);
                this.$el.find(".tab-menu .nav-tabs").tabdrop();
                this.setTabId(this.options.tabs[0].id);
            });
            return this;
        }
    });

    return TabComponent;
});